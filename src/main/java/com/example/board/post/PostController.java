package com.example.board.post;

import com.example.board.global.IngestResult;
import com.example.board.post.dto.PostRequest;
import com.example.board.post.dto.PostDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

            @SessionAttribute(name = LOGIN_USER_ID, required = false)
            Long loginUserId,

            @Valid
            @RequestBody
            PostRequest request){
        return postService.create(boardId, loginUserId,request);

    }

    @GetMapping("/all")
    public List<PostDTO> list(){
        return postService.list();
    }
    //모든 사용자 가능
    @GetMapping("{postId}")
    public PostDTO getPost(
            @PathVariable Long postId){
        return postService.getPost(postId);
    }

    //작성자만 가능
    @PutMapping("/{postId}/update")
    public PostDTO update(
        @PathVariable(name = "postId") Long id,

        @SessionAttribute(name = LOGIN_USER_ID, required = false)
        Long loginUserId,

        @Valid
        @RequestBody
        PostRequest request
    ){
        return postService.update(loginUserId, id, request);
    }

    //작성자만 가능
    @DeleteMapping("/{postId}/delete")
    public void delete(
            @PathVariable Long postId,

            @SessionAttribute(name = LOGIN_USER_ID, required = false)
            Long loginUserId
    ){
        postService.delete(postId,loginUserId);
    }
}
