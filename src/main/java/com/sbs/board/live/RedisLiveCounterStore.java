package com.sbs.board.live;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisLiveCounterStore implements LiveCounterStore{

    static final long TTL_SECONDS = Duration.ofDays(7).toSeconds();

    private static final String TOGGLE_LIKE = """
            local like
            local score
            if redis.call('SADD',KEYS[1], ARGV[1]) == i then
                liked = 1
                score = redis.call('ZINCRBY', KEYS[2], 1, ARGV[1])
            else
                redis.call('SREM', KEYS[1], ARGV[1])
                liked = 0
                score = redis.call('ZINCRBY', KEYS[2], -1, ARGV[1])
            end
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            redis.call('EXPIRE', KEYS[2], ARGV[2])
            return {liked, tonumber(score)}
            """;

    private static final String VOTE_SCR = """
         if redis.call('HSETNX', KEYS[1], ARGV[1], ARGV[2]) === 0 then
            return 0
         end
         redis.call('HINCRBY', KEYS[2], ARGV[2], 1)
         redis.call('EXPIRE', KEYS[1], ARGV[3])
         redis.call('EXPIRE', KEYS[2], ARGV[3])
         return 1
         """;

    private final StringRedisTemplate redis;

    private static final RedisScript<List> toggleLike = new DefaultRedisScript<>(TOGGLE_LIKE, List.class);
    private static final RedisScript<Long> VOTE = new DefaultRedisScript<>(VOTE_SCR, Long.class);

    private static String questionsKey(Long eventId){
        return "live:event:" + eventId + ":question";
    }

    private static String userLikesKey(Long eventId,Long userId){
        return "live:event:" + eventId + ":user:" + userId + ":likes:";
    }

    private static long toLong(Object value){
        return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
    }

    @Override
    public void addQuestion(Long eventId, Long questionId) {

        String key =questionsKey(eventId);
        redis.opsForZSet().add(key, questionId.toString(),0);
        redis.expire(key, Duration.ofSeconds(TTL_SECONDS));
    }

    @Override
    public void removeQuestion(Long eventId, Long questionId) {
        redis.opsForZSet().remove(questionsKey(eventId), questionId.toString());
    }

    @Override
    public LikeResult toggleLike(Long eventId, Long questionId, Long userId) {

        List<?> result = redis.execute(toggleLike,
                List.of(userLikesKey(eventId, userId), questionsKey(eventId)),
                questionId.toString(), String.valueOf(TTL_SECONDS));

        if( result == null || result.size() != 2){
            throw new IllegalStateException("toggle like script returned "+result);
        }

        return new LikeResult(toLong(result.get(0))==1L,toLong(result.get(1)));

    }

    @Override
    public Map<Long, Long> likeCounts(Long eventId) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis
                .opsForZSet()
                .rangeWithScores(questionsKey(eventId), 0 ,-1);

        Map<Long, Long> counts = new HashMap<>();
        if(tuples != null){
            for (ZSetOperations.TypedTuple<String> tuple : tuples){
                Double score = tuple.getScore();
                counts.put(Long.valueOf(tuple.getValue()), score == null ? 0L : score.longValue());
            }
        }

        return counts;
    }

    @Override
    public Set<Long> likedQuestionIds(Long eventId, Long userId) {
        Set<String> members = redis.opsForSet().members(userLikesKey(eventId, userId));
        if (members == null){
            return Set.of();
        }

        return members.stream().map(Long::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    private static String countsKey(Long pollId) {
        return "live:poll:" + pollId + ":counts";
    }

    private static String votersKey(Long pollId){
        return "live:poll:" + pollId + ":voters:";
    }


    @Override
    public boolean vote(Long pollId, Long optionId, Long userId) {
        Long result = redis.execute(VOTE,
                List.of(votersKey(pollId), countsKey(pollId)),
                userId.toString(),
                optionId.toString(),
                String.valueOf(TTL_SECONDS)
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public Map<Long, Long> voteCount(Long pollId) {
        Map<Object, Object> entries = redis.opsForHash().entries(countsKey(pollId));
        Map<Long, Long> result = new HashMap<>();

        entries.forEach((option, count)->
                result.put(Long.valueOf(option.toString()),Long.valueOf(count.toString())));
        return result;
    }

    @Override
    public Long voteOptionId(Long pollId, Long userId) {
        Object optionId = redis.opsForHash().get(votersKey(pollId), userId.toString());

        return optionId == null ? null : Long.valueOf(optionId.toString());
    }
}
