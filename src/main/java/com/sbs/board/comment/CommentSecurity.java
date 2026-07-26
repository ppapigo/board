package com.sbs.board.comment;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("commentSecurity")
@RequiredArgsConstructor
public class CommentSecurity {
    private final CommentRepository commentRepository;

    public boolean isAuthor(Long commentId, CustomUserDetails userDetails){
        Long authorId = commentRepository.findAuthorById(commentId)
                .orElseThrow(()-> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));

        return authorId.equals(userDetails.getId());
    }
}
