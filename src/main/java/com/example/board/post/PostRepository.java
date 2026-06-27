package com.example.board.post;

import com.example.board.global.entity.Board;
import com.example.board.global.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByBoard(Board board);
    List<Post> findByBoardId(Long boardId);
}
