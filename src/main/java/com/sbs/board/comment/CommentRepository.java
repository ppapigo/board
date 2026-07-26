package com.sbs.board.comment;

import com.sbs.board.global.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // select * from comments
    // join user u on c.user_id = u.id
    // where post_id =:postId parent_id is null
    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findByPostIdAndParentIsNull(Long postId, Pageable pageable);

    @Query("""
    select c.user.id
    from Comment c
    where c.id = :commentId
""")
    Optional<Long> findAuthorById(@Param("commentId") Long commentId);

}
