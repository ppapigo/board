package com.sbs.board.post;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.auth.LoginUserId;
import com.sbs.board.global.IngestResult;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.post.dto.PostRequest;
import com.sbs.board.post.dto.PostDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.sbs.board.auth.AuthController.LOGIN_USER_ID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostController {
    private static final int MAX_IMAGES = 5;
    private final PostService postService;

    @PostMapping("/{boardId}/new")
    public PostDTO create(
            @PathVariable
            Long boardId,

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @Valid @RequestPart("post") PostRequest post,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
            ) {
        validateImageCount(images);
        return postService.create(boardId, userDetails.getId(), post, images);
    }

    private void validateImageCount(List<MultipartFile> images){
        if( images != null && images.size() > MAX_IMAGES){
            throw new BusinessException(ErrorCode.FILE_COUNT_EXCEEDED);
        }
    }

    // 모든 사용자 가능
    @GetMapping("/all")
    public List<PostDTO> list() {
        return postService.list();
    }

    // Board Id를 받아서 해당 Board의 모든 게시글을 반환하는 기능
    @GetMapping("/{boardId}/all")
    public Page<PostDTO> findByBoard(@PathVariable Long boardId,
                                     @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable) {
        return postService.findByBoardId(boardId, pageable);
    }

    // id로 PostDTO 한개 반환하기, 모든 사용자 가능
    @GetMapping("/{id}")
    public PostDTO getPost(@PathVariable Long id,
                           @AuthenticationPrincipal
                           CustomUserDetails userDetails) {

        return postService.getPost(userDetails.getId(), id);
    }

    // update, 작성자만 가능
    @PreAuthorize("@postSecurity.isAuthor(#id, authentication.principal)")
    @PutMapping("/{id}/update")
    public PostDTO update(
        @PathVariable Long id,

        @Valid
        @RequestBody
        PostRequest request
    ) {
        return postService.update(id, request);
    }

    // delete, 작성자만 가능
    @PreAuthorize("@postSecurity.isAuthor(#id, authentication.principal)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ) {
        postService.delete(id);

        return ResponseEntity.status(HttpStatus.OK).body("ok");
    }
}
