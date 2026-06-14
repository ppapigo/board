package com.example.board.entity;


import com.example.board.board.dto.BoardDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name="created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name="updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();


    public static BoardDTO toDTO(Board board) {
        return new BoardDTO(board.getId(), board.getName(), board.getDescription());
    }
}
