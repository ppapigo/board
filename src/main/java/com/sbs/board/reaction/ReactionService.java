package com.sbs.board.reaction;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.comment.CommentRepository;
import com.sbs.board.global.entity.Comment;
import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.post.PostRepository;
import com.sbs.board.reaction.dto.ReactionResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final PostReactionRepository postReactionRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public ReactionResponse react(Long postId, Long userId, @NotNull ReactionType type) {
        // post가 있는지 확인하고 없으면 에러 발생
        if(!postRepository.existsById(postId)){
            throw new NotFoundException(ErrorCode.POST_NOT_FOUND);
        }
        // postId와 userId를 이용하여 reaction이 존재하는지 여부 확인
        //존재하면 이미 사용자가 react를 한 post이므로 type만 수정됨
        //존재하지 않으면 반응하지 않은 post이므로 reaction record를 생성함
        postReactionRepository.findByPostIdAndUserId(postId, userId).ifPresentOrElse(
                (reaction)-> {
                    // type과 postReaction의 타입이 동일하면 취소(해당 reaction 삭제)
                    if (type == reaction.getType()) {
                        postReactionRepository.delete(reaction);
                    } else
                        reaction.changeType(type);
                    },

                ()->{
                    //findById 와 getReferenceById
                    //findById 는 반환값이 Optional<T>로 null safety 객체를 반환
                    //getReferenceById 는 인스턴스를 즉시 반환 -> exception 발생 가능

                    Post post =postRepository.getReferenceById(postId);
                    User user= userRepository.getReferenceById(userId);
                    postReactionRepository.save(PostReaction.builder()
                            .post(post)
                            .user(user)
                            .type(type)
                            .build());
                }
        );
        // ReactionResponse 인스턴스를 생성하여 반환
        // postReactionRepository의 countByPostIdAndType()을 이용하여
        // Like, DisLike의 개수를 확인할 수 있음
        // findByPostIdAndUserId()를 이용하여 해당 사용자의  postReaction을 가져올 수 있음
        // 위 3개의 정보를 이용하여 ReactionResponse 인스턴스를 생성할 수 있음
        return buildPostReactionResponse(postId, userId);

    }
    public ReactionResponse buildPostReactionResponse(Long postId, Long userId){
        //해당 게시글(postId)의 좋아요(like)와 싫어요(dislike) 개수 정보 구한다.
        long like = postReactionRepository.countByPostIdAndType(postId, ReactionType.LIKE);
        long dislike = postReactionRepository.countByPostIdAndType(postId, ReactionType.DISLIKE);

        //reaction 레코드를 가져와서 getType()르호 ReactionType릏 가져오거나 없으면 null로 반환
        ReactionType myReaction = postReactionRepository.findByPostIdAndUserId(postId, userId)
                .map(PostReaction::getType)
                .orElse(null);

        //reactionResponse 인스턴스 반환
        return new ReactionResponse(like, dislike, myReaction);
    }

    @Transactional
    public ReactionResponse reactToComment(Long commentId, Long userId, @NotNull ReactionType type) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        }

        commentReactionRepository.findByCommentIdAndUserId(commentId, userId)
                .ifPresentOrElse(
                        reaction -> {
                            if (reaction.getType() == type) {
                                commentReactionRepository.delete(reaction);
                            } else {
                                reaction.changeType(type);
                            }
                        },
                        () -> {
                            Comment comment = commentRepository.getReferenceById(commentId);
                            User user = userRepository.getReferenceById(userId);
                            commentReactionRepository.save(CommentReaction.builder()
                                    .comment(comment)
                                    .user(user)
                                    .type(type)
                                    .build());
                        }
                );

        return buildCommentReactionResponse(commentId, userId);
    }

    public ReactionResponse buildCommentReactionResponse(Long commentId, Long userId) {
        long like = commentReactionRepository.countByCommentIdAndType(commentId, ReactionType.LIKE);
        long dislike = commentReactionRepository.countByCommentIdAndType(commentId, ReactionType.DISLIKE);
        ReactionType myReaction = userId == null
                ? null
                : commentReactionRepository.findByCommentIdAndUserId(commentId, userId)
                        .map(CommentReaction::getType)
                        .orElse(null);

        return new ReactionResponse(like, dislike, myReaction);
    }

    @Transactional(readOnly = true)
    public Map<Long, ReactionResponse> buildCommentReactionResponses(
            Collection<Long> commentIds,
            Long userId
    ) {
        Map<Long, ReactionResponse> responses = new HashMap<>();
        commentIds.forEach(id -> responses.put(id, new ReactionResponse(0, 0, null)));
        if (commentIds.isEmpty()) {
            return responses;
        }

        commentReactionRepository.countByCommentIdsAndType(commentIds).forEach(row -> {
            Long commentId = (Long) row[0];
            ReactionType type = (ReactionType) row[1];
            long count = ((Number) row[2]).longValue();
            ReactionResponse response = responses.get(commentId);

            if (type == ReactionType.LIKE) {
                response.setLikeCount(count);
            } else {
                response.setDislikeCount(count);
            }
        });

        if (userId != null) {
            commentReactionRepository.findByCommentIdInAndUserId(commentIds, userId)
                    .forEach(reaction -> responses.get(reaction.getComment().getId())
                            .setMyReaction(reaction.getType()));
        }

        return responses;
    }
}
