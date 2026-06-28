package com.example.board.post;

import com.example.board.auth.CustomUserDetails;
import com.example.board.auth.LoginUserId;
import com.example.board.global.IngestResult;
import com.example.board.global.entity.Board;
import com.example.board.post.dto.PostRequest;
import com.example.board.post.dto.PostDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.board.auth.AuthController.LOGIN_USER_ID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;

    @PostMapping("/{boardId}/new")
    public PostDTO create(
            @PathVariable
            Long boardId,

           @AuthenticationPrincipal
           CustomUserDetails userDetails,

            @Valid
            @RequestBody
            PostRequest request){
        return postService.create(boardId, userDetails.getId(), request);

    }

    @GetMapping("/all")
    public List<PostDTO> list(){
        return postService.list();
    }

    @GetMapping("/{boardId}/all")
    public List<PostDTO> findByBoard(@PathVariable Long boardId){
        return postService.findByBoardId(boardId);
    }

    //모든 사용자 가능
    @GetMapping("{Id}")
    public PostDTO getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal
            CustomUserDetails userDetails){
        return postService.getPost(userDetails.getId(), id);
    }

    //작성자만 가능
    @PreAuthorize("@postSecurity.isAuthor(#id, authentication.principal)")
    @PutMapping("/{postId}/update")
    public PostDTO update(
        @PathVariable(name = "postId") Long id,

       @AuthenticationPrincipal
       CustomUserDetails userDetails,

        @Valid
        @RequestBody
        PostRequest request
    ){
        return postService.update( id, request);
    }

    //작성자만 가능
    @PreAuthorize("@postSecurity.isAuthor(#id, authentication.principal)")
    @DeleteMapping("/{postId}/delete")
    public void delete(
            @PathVariable Long id
    ){
        postService.delete(id);
    }
}
