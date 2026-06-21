package com.example.board.board;

import com.example.board.auth.LoginUserId;
import com.example.board.board.dto.BoardDTO;
import com.example.board.board.dto.BoardRequest;
import com.example.board.board.dto.BoardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.board.auth.AuthController.LOGIN_USER_ID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {
    private final BoardService boardService;

    @PostMapping("/new")
    public BoardResponse create(
            @LoginUserId
            Long loginUserId,

            @Valid
            @RequestBody
            BoardRequest request){
        return boardService.create(loginUserId,request);
    }

    @GetMapping("/all")
    public List<BoardResponse> list(){
        return boardService.list();
    }

    @PutMapping("/{id}/update")
    public BoardResponse update(
            @PathVariable Long id,

            @LoginUserId
            Long loginUserId,

            @RequestBody BoardRequest boardRequest){
        return boardService.update(id,loginUserId,boardRequest);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id,

            @LoginUserId
            Long loginUserId
    ) {
        String result = boardService.delete(id, loginUserId);
        return ResponseEntity.status(HttpStatus.OK).body((result));
    }
}
