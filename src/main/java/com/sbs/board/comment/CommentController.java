package com.sbs.board.comment;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.comment.dto.CommentCreateRequest;
import com.sbs.board.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;


    @PostMapping("/posts/{postId}/new")
    public CommentResponse create(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommentCreateRequest request
            ){

        return commentService.create(postId, userDetails.getId(), request);
    }



  @GetMapping("post/{postId}/list")
    public Page<CommentResponse> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable
            ){

        return commentService.getComment(postId,pageable);
    }

    //작성자만 수정이 가능하게
    @PreAuthorize("@commentSecurity.isAuthor(#id, authentication.principal)")
    @PutMapping("/{id}")
    public CommentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateRequest request
    ){
        return commentService.update(id, request);
    }


    @PreAuthorize("@commentSecurity.isAuthor(#id, authentication.principal)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        // 삭제 코드
        commentService.delete(id);
        return ResponseEntity.ok().build();
    }

}
