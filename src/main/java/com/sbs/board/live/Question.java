package com.sbs.board.live;

import com.sbs.board.global.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "live_questions",
        indexes =@Index(name="idx_Live_question_event", columnList = "event_id") )
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id",nullable = false)
    private LiveEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(name="created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at")
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Question(LiveEvent event, User author, String content){
        this.event=event;
        this.author=author;
        this.content=content;
    }

    public boolean isAuthorBy(Long userId){
        return Objects.equals(author.getId(),userId);
    }
}
