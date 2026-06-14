package com.example.board.board;

import com.example.board.auth.UserRepository;
import com.example.board.board.dto.BoardDTO;
import com.example.board.board.dto.CreateBoard;
import com.example.board.board.dto.CreateBoardResponse;
import com.example.board.entity.Board;
import com.example.board.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public void validateUser(Long loginUserId){
        User user = new User();
        user = userRepository.findById(loginUserId)
                .orElseThrow(()->new RuntimeException("사용자를 찾을 수 없습니다"));

        if(user.getRole() != User.Role.ADMIN){
            throw new RuntimeException("게시판을 생성할 권한이 없는 사용자입니다.");
        }
    }
    // Create
    @Transactional
    public CreateBoardResponse create(Long loginUserId,CreateBoard request){
        validateUser(loginUserId);

        if(boardRepository.existsByName(request.getName())){
            // return error

        }
        Board board = new Board();
        board.setName(request.getName());
        board.setDescription(request.getDescription());


        Board savedBoard = boardRepository.save( board );

        CreateBoardResponse response = new CreateBoardResponse();
        response.setId(savedBoard.getId());
        response.setName(savedBoard.getName());
        response.setDescription(savedBoard.getDescription());
        response.setCreatedAt(savedBoard.getCreatedAt().toString());

        return response;
    }

    //Read
    @Transactional(readOnly = true)
    public List<BoardDTO> list(){
        return boardRepository.findAll().stream().map(Board::toDTO).toList();
    }
    //Update

    //Delete
}
