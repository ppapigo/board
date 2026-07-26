package com.sbs.board.comment;


import com.sbs.board.auth.UserRepository;
import com.sbs.board.comment.dto.CommentCreateRequest;
import com.sbs.board.comment.dto.CommentResponse;
import com.sbs.board.global.entity.Comment;
import com.sbs.board.global.entity.Post;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.notification.CommentCreateEvent;
import com.sbs.board.post.PostRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CommentService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CommentResponse create(Long postId, Long userId,CommentCreateRequest request){

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));


        Comment comment = new Comment();

        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(user);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));

            if( !parent.getPost().getId().equals(postId)){
                throw new BusinessException(ErrorCode.COMMENT_POST_MISMATCH);
            }
            if(parent.isDeleted()){
                throw new BusinessException(ErrorCode.CANNOT_REPLY_TO_DELETED);
            }
            if(parent.isReply()){
                throw new BusinessException(ErrorCode.CANNOT_REPLY_TO_REPLY);
            }

            comment.setParent(parent);
        }

        Comment savedComment = commentRepository.save(comment);

        //이벤트를 발생시킴
        eventPublisher.publishEvent( new CommentCreateEvent(
                savedComment.getId(),
                postId,
                request.getParentId(),
                userId
        ));

        CommentResponse response = new CommentResponse();
        response.setId(savedComment.getId());
        response.setContent(savedComment.getContent());
        response.setParent(
                savedComment.getParent() != null
                        ? savedComment.getParent().getId()
                        : null
        );
        response.setAuthorUserName(savedComment.getUser().getNickName());
        response.setCreatedAt(savedComment.getCreatedAt());




        return response;
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComment(Long postId, Pageable pageable) {

        if (!postRepository.existsById(postId)) {
            throw new NotFoundException(ErrorCode.POST_NOT_FOUND);
        }

        return commentRepository.findByPostIdAndParentIsNull(postId, pageable)
                .map(Comment::toResponse);
    }

    public CommentResponse update(Long id, @Valid CommentCreateRequest request) {
        Comment comment =commentRepository.findById(id)
                .orElseThrow(()->new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        if(comment.isDeleted()){
            throw new BusinessException(ErrorCode.CANNOT_EDIT_DELETED);
        }
        comment.setContent(request.getContent());
        Comment savedComment = commentRepository.save(comment);

        return Comment.toResponse(savedComment);
    }

    public void delete(Long id) {
        Comment comment =commentRepository.findById(id)
                .orElseThrow(()->new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));


        comment.softDelete();
        commentRepository.save(comment);

    }
}
