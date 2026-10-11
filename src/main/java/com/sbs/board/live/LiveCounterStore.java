package com.sbs.board.live;

import java.util.Map;
import java.util.Set;

public interface LiveCounterStore {
    void addQuestion(Long eventId, Long questionId);

    void removeQuestion(Long eventId, Long questionId);

    LikeResult toggleLike(Long eventId, Long questionId, Long userId);

    Map<Long, Long> likeCounts(Long eventId);

    Set<Long> likedQuestionIds(Long eventId, Long userId);

    boolean vote(Long pollId, Long optionId, Long userId);
    Map<Long, Long> voteCount(Long pollId);
    Long voteOptionId(Long pollId, Long userId);
}
