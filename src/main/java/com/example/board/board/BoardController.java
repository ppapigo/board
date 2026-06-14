package com.example.board.board;

import com.example.board.board.dto.BoardDTO;
import com.example.board.board.dto.CreateBoard;
import com.example.board.board.dto.CreateBoardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.board.auth.AuthController.LOGIN_USER_ID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {
    private final BoardService boardService;

    @PostMapping("/new")
    public CreateBoardResponse create(
            @SessionAttribute(name = LOGIN_USER_ID, required = false)
            Long loginUserId,

            @Valid
            @RequestBody
            CreateBoard request){
        return boardService.create(loginUserId,request);
    }

    @GetMapping("/all")
    public List<BoardDTO> list(){
        return boardService.list();
    }
}
