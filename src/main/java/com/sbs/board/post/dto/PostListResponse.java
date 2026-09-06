package com.sbs.board.post.dto;

import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.PostImage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostListResponse {
    private Long id;

    private String title;

    private String author;

    private long viewCount;

    private String thumbnailUrl;

    private LocalDateTime createdAt;
    
    public static PostListResponse from(Post post){
        String thumbnail = post.getImages()
                .stream().findFirst()
                .map(PostImage::getStoredName)
                .map(name-> PostImage.URL_PREFIX +name)
                .orElse(null);
        
        return new PostListResponse(
                post.getId(),
                post.getTitle(), 
                post.getAuthor().getNickName(),
                post.getViewCount(),
                thumbnail,
                post.getCreatedAt()
        );
    }
}
