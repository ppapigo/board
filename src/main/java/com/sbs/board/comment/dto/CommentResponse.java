package com.sbs.board.comment.dto;


import com.sbs.board.reaction.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String authorUserName;
    private String content;
    private Long parent;
    private LocalDateTime createdAt;
    private boolean deleted;
    private List<CommentResponse> children;
    private long likeCount;
    private long dislikeCount;
    private ReactionType myReaction;

    public static final String DELETED_CONTENT = "삭제된 댓글입니다.";
}
