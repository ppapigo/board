package com.example.board.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "board")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title",length = 100, nullable = false)
    private String title;

    @Column(name = "body",length = 500, nullable = false)
    private String body;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name="created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt = LocalDateTime.now();

}

