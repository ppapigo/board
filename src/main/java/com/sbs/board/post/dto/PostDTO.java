package com.sbs.board.post.dto;

import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.PostImage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private String title;
    private String author;
    private String board;
    private String body;
    private long viewCount;
    List<PostImageResponse> images;
    private String createdAt;
    private boolean canEdit;
    private boolean canDelete;
}
