package com.sbs.board.post.dto;

import com.sbs.board.global.entity.Post;
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
public class PostCursorResponse {
    List<PostListResponse> items;
    boolean hasNext;
    LocalDateTime lastCreatedAt;
    Long lastId;

    public static PostCursorResponse of(List<Post> rows, int size){
        boolean hasNext = rows.size() > size;
        List<Post> pageRows = hasNext ? rows.subList(0,size) : rows;
        List<PostListResponse> items = pageRows.stream().map(PostListResponse::from).toList();
        Post last = pageRows.isEmpty() ? null : pageRows.get(pageRows.size() -1);
        return new PostCursorResponse(
                items,
                hasNext,
                last == null ? null : last.getCreatedAt(),
                last == null ? null : last.getId()
        );
    }
}
