package com.example.board.post;

import com.example.board.global.IngestResult;
import com.example.board.post.dto.CreatePost;
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
    public IngestResult create(
            @PathVariable
            Long boardId,

            @SessionAttribute(name = LOGIN_USER_ID, required = false)
            Long loginUserId,

            @Valid
            @RequestBody
            CreatePost request){
        return postService.create(boardId, loginUserId,request);

    }

    @GetMapping("/all")
    public List<PostDTO> list(){
        return postService.list();
    }
}
