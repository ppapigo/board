package com.sbs.board.live;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// Uses a dedicated local Redis server; only keys created by this test are removed.
@EnabledIfEnvironmentVariable(named = "TEST_REDIS_PORT", matches = "[0-9]+")
class RedisLiveCounterStoreTest {
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private RedisLiveCounterStore store;
    private Long eventId;
    private Long pollId;
    private final List<String> keys = new ArrayList<>();

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory("127.0.0.1",
                Integer.parseInt(System.getenv("TEST_REDIS_PORT")));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        assertThat(redis.getConnectionFactory()).isNotNull();
        try (var connection = connectionFactory.getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
    }

    @BeforeEach
    void setUp() {
        store = new RedisLiveCounterStore(redis);
        eventId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        pollId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        keys.add(questionsKey());
        keys.add(votersKey());
        keys.add(countsKey());
    }

    @AfterEach
    void removeTestKeys() {
        redis.delete(keys);
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void likeCanBeAddedRemovedAndAddedAgain() {
        trackUser(10L);
        store.addQuestion(eventId, 1L);

        assertThat(store.toggleLike(eventId, 1L, 10L)).isEqualTo(new LikeResult(true, 1));
        assertThat(store.likedQuestionIds(eventId, 10L)).containsExactly(1L);
        assertTtl(questionsKey());
        assertTtl(userLikesKey(10L));

        assertThat(store.toggleLike(eventId, 1L, 10L)).isEqualTo(new LikeResult(false, 0));
        assertThat(store.likedQuestionIds(eventId, 10L)).isEmpty();
        assertThat(store.likeCounts(eventId)).containsEntry(1L, 0L);

        assertThat(store.toggleLike(eventId, 1L, 10L)).isEqualTo(new LikeResult(true, 1));
        assertThat(store.likeCounts(eventId)).containsEntry(1L, 1L);
    }

    @Test
    void likesAreIsolatedByUserAndQuestion() {
        trackUser(10L);
        trackUser(20L);
        store.addQuestion(eventId, 1L);
        store.addQuestion(eventId, 2L);

        store.toggleLike(eventId, 1L, 10L);
        store.toggleLike(eventId, 1L, 20L);
        store.toggleLike(eventId, 2L, 10L);
        assertThat(store.likeCounts(eventId)).containsEntry(1L, 2L).containsEntry(2L, 1L);
        assertThat(store.likedQuestionIds(eventId, 10L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(store.likedQuestionIds(eventId, 20L)).containsExactly(1L);

        assertThat(store.toggleLike(eventId, 1L, 10L)).isEqualTo(new LikeResult(false, 1));
        assertThat(store.likeCounts(eventId)).containsEntry(1L, 1L).containsEntry(2L, 1L);
    }

    @Test
    void eachUserCanVoteOnlyOnceEvenForADifferentOption() {
        assertThat(store.vote(pollId, 1L, 10L)).isTrue();
        assertThat(store.vote(pollId, 1L, 10L)).isFalse();
        assertThat(store.vote(pollId, 2L, 10L)).isFalse();
        assertThat(store.voteOptionId(pollId, 10L)).isEqualTo(1L);
        assertThat(store.voteCount(pollId)).hasSize(1).containsEntry(1L, 1L);

        assertThat(store.vote(pollId, 2L, 20L)).isTrue();
        assertThat(store.voteCount(pollId)).hasSize(2).containsEntry(1L, 1L).containsEntry(2L, 1L);
        assertTtl(votersKey());
        assertTtl(countsKey());
    }

    @Test
    void concurrentDuplicateVotesAreCountedOnce() throws Exception {
        List<Callable<Boolean>> votes = IntStream.range(0, 24)
                .mapToObj(index -> (Callable<Boolean>) () -> store.vote(pollId, (long) (index % 2 + 1), 10L))
                .toList();

        try (var executor = Executors.newFixedThreadPool(8)) {
            int accepted = 0;
            for (var result : executor.invokeAll(votes)) {
                if (result.get()) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(1);
        }
        assertThat(store.voteCount(pollId)).hasSize(1)
                .containsEntry(store.voteOptionId(pollId, 10L), 1L);
    }

    @Test
    void concurrentLikesAndUnlikesKeepExactCounts() throws Exception {
        store.addQuestion(eventId, 1L);
        List<Callable<LikeResult>> likes = IntStream.range(10, 34)
                .peek(userId -> trackUser((long) userId))
                .mapToObj(userId -> (Callable<LikeResult>) () -> store.toggleLike(eventId, 1L, (long) userId))
                .toList();

        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var result : executor.invokeAll(likes)) {
                assertThat(result.get().liked()).isTrue();
            }
            assertThat(store.likeCounts(eventId)).containsEntry(1L, 24L);

            for (var result : executor.invokeAll(likes)) {
                LikeResult like = result.get();
                assertThat(like.liked()).isFalse();
                assertThat(like.likeCount()).isNotNegative();
            }
        }
        assertThat(store.likeCounts(eventId)).containsEntry(1L, 0L);
    }

    private void assertTtl(String key) {
        assertThat(redis.getExpire(key)).isBetween(1L, RedisLiveCounterStore.TTL_SECONDS);
    }

    private void trackUser(Long userId) {
        keys.add(userLikesKey(userId));
    }

    private String questionsKey() {
        return "live:event:" + eventId + ":question";
    }

    private String userLikesKey(Long userId) {
        return "live:event:" + eventId + ":user:" + userId + ":likes:";
    }

    private String votersKey() {
        return "live:poll:" + pollId + ":voters:";
    }

    private String countsKey() {
        return "live:poll:" + pollId + ":counts";
    }
}
