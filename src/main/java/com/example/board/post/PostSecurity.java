package com.example.board.post;

import com.example.board.auth.CustomUserDetails;
import com.example.board.global.exception.ErrorCode;
import com.example.board.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("postSecurity")
@RequiredArgsConstructor
public class PostSecurity {

    private final PostRepository postRepository;

    public boolean isAuthor(Long postId, CustomUserDetails user){

        log.debug("PostSecurity.isAuthor user.email: {}",user.getUsername());

        Long authorId = postRepository.findAuthorById(postId)
                .orElseThrow(()-> new NotFoundException(ErrorCode.POST_NOT_FOUND));

        return authorId.equals(user.getId());
    }
}
