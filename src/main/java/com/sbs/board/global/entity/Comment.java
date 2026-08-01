package com.sbs.board.global.entity;


import com.sbs.board.comment.dto.CommentResponse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "comment")
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "parent")
    @OrderBy("createdAt asc")
    @BatchSize(size = 100)
    private List<Comment> children = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public void addReply(Comment reply){
        children.add( reply );
        reply.parent = this;
    }

    public void update(String content){
        this.content = content;
    }

    public void softDelete(){
        this.deleted = true;
    }

    public boolean isRoot(){
        return parent == null;
    }

    public boolean isReply(){
        return parent != null;
    }

    public boolean isAuthor(Long userId){
        return user.getId().equals(userId);
    }

    public static CommentResponse toResponse(Comment comment){
        List<CommentResponse> children = comment.isRoot() && comment.getChildren()!=null
                ? comment.getChildren().stream().map(Comment::toResponse).toList()
                : List.of();

        return new CommentResponse(
                comment.getId(),
                comment.getUser().getNickName(),
                comment.isDeleted() ? CommentResponse.DELETED_CONTENT : comment.getContent(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getCreatedAt(),
                comment.isDeleted(),
                children,
                0,
                0,
                null
        );
    }
}
// id
// post_id
// user_id
// parent_id
// content  String(Text)
// deleted  boolean(Soft delete)
// created_at LocalDateTime
