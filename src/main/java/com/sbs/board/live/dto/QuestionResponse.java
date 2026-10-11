package com.sbs.board.live.dto;

import com.sbs.board.live.Question;

import java.time.LocalDateTime;
import java.util.Comparator;

public record QuestionResponse(
        Long id,
        String content,
        String authorUsername,
        long likeCount,
        boolean likedByMe,
        boolean mine,
        LocalDateTime createdAt
) {
    public static final Comparator<QuestionResponse> RANKING =
            Comparator.comparingLong(QuestionResponse::likeCount).reversed()
                    .thenComparing(QuestionResponse::id, Comparator.reverseOrder());

    public static QuestionResponse of(Question question, long likeCount, boolean likedByMe, Long viewerId){

        return new QuestionResponse(
                question.getId(),
                question.getContent(),
                question.getAuthor().getEmail(),
                likeCount,
                likedByMe,
                viewerId != null && question.isAuthorBy(viewerId),
                question.getCreatedAt()
        );
    }


}
