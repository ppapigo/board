package com.sbs.board.reaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);

    //게시글의 Like/DisLike 개수
    long countByPostIdAndType(Long postId, ReactionType type);
}

