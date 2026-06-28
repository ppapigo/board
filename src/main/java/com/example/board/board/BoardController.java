package com.example.board.board;

import com.example.board.auth.LoginUserId;
import com.example.board.board.dto.BoardDTO;
import com.example.board.board.dto.BoardRequest;
import com.example.board.board.dto.BoardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.board.auth.AuthController.LOGIN_USER_ID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {
    private final BoardService boardService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/new")
    public BoardResponse create(
            @Valid
            @RequestBody
            BoardRequest request){
        return boardService.create(request);
    }

    @GetMapping("/all")
    public List<BoardResponse> list(){
        return boardService.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/update")
    public BoardResponse update(
            @PathVariable Long id,

            @RequestBody BoardRequest boardRequest){
        return boardService.update(id,boardRequest);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id
    ) {
        String result = boardService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body((result));
    }
}
