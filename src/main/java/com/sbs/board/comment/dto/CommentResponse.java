package com.sbs.board.comment.dto;


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

    public static final String DELETED_CONTENT = "삭제된 댓글입니다.";

    public CommentResponse(Long id, String nickName, String content, String s, LocalDateTime createdAt, List<CommentResponse> children) {
    }
}
