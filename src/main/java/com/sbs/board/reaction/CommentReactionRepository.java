package com.sbs.board.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    //게시글의 Like/DisLike 개수
    long countByCommentIdAndType(Long commentId, ReactionType type);

    @Query("""
            select cr.comment.id, cr.type, count(cr)
            from CommentReaction cr
            where cr.comment.id in :commentIds
            group by cr.comment.id, cr.type
            """)
    List<Object[]> countByCommentIdsAndType(@Param("commentIds") Collection<Long> commentIds);

    List<CommentReaction> findByCommentIdInAndUserId(Collection<Long> commentIds, Long userId);
}
