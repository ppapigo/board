package com.sbs.board.reaction;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.comment.CommentRepository;
import com.sbs.board.global.entity.Comment;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.post.PostRepository;
import com.sbs.board.reaction.dto.ReactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {
    @Mock PostReactionRepository postReactionRepository;
    @Mock CommentReactionRepository commentReactionRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock UserRepository userRepository;

    private ReactionService reactionService;

    @BeforeEach
    void setUp() {
        reactionService = new ReactionService(postReactionRepository, commentReactionRepository,
                postRepository, commentRepository, userRepository);
    }

    @Test
    void createsCommentReactionAndReturnsCurrentCounts() {
        long commentId = 10L;
        long userId = 20L;
        Comment comment = new Comment();
        User user = new User();

        when(commentRepository.existsById(commentId)).thenReturn(true);
        when(commentReactionRepository.findByCommentIdAndUserId(commentId, userId))
                .thenReturn(Optional.empty(), Optional.of(CommentReaction.builder().type(ReactionType.LIKE).build()));
        when(commentRepository.getReferenceById(commentId)).thenReturn(comment);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(commentReactionRepository.countByCommentIdAndType(commentId, ReactionType.LIKE)).thenReturn(3L);
        when(commentReactionRepository.countByCommentIdAndType(commentId, ReactionType.DISLIKE)).thenReturn(1L);

        ReactionResponse response = reactionService.reactToComment(commentId, userId, ReactionType.LIKE);

        verify(commentReactionRepository).save(argThat(reaction -> reaction.getComment() == comment
                && reaction.getUser() == user && reaction.getType() == ReactionType.LIKE));
        assertThat(response.getLikeCount()).isEqualTo(3L);
        assertThat(response.getDislikeCount()).isEqualTo(1L);
        assertThat(response.getMyReaction()).isEqualTo(ReactionType.LIKE);
    }

    @Test
    void sameCommentReactionCancelsExistingReaction() {
        long commentId = 10L;
        long userId = 20L;
        CommentReaction existing = CommentReaction.builder().type(ReactionType.LIKE).build();

        when(commentRepository.existsById(commentId)).thenReturn(true);
        when(commentReactionRepository.findByCommentIdAndUserId(commentId, userId))
                .thenReturn(Optional.of(existing), Optional.empty());

        ReactionResponse response = reactionService.reactToComment(commentId, userId, ReactionType.LIKE);

        verify(commentReactionRepository).delete(existing);
        assertThat(response.getMyReaction()).isNull();
    }

    @Test
    void differentCommentReactionChangesExistingType() {
        long commentId = 10L;
        long userId = 20L;
        CommentReaction existing = CommentReaction.builder().type(ReactionType.LIKE).build();

        when(commentRepository.existsById(commentId)).thenReturn(true);
        when(commentReactionRepository.findByCommentIdAndUserId(commentId, userId))
                .thenReturn(Optional.of(existing), Optional.of(existing));

        ReactionResponse response = reactionService.reactToComment(commentId, userId, ReactionType.DISLIKE);

        assertThat(existing.getType()).isEqualTo(ReactionType.DISLIKE);
        assertThat(response.getMyReaction()).isEqualTo(ReactionType.DISLIKE);
        verify(commentReactionRepository, never()).delete(existing);
    }

    @Test
    void missingCommentIsRejected() {
        when(commentRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> reactionService.reactToComment(10L, 20L, ReactionType.LIKE))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(commentReactionRepository);
    }
}
