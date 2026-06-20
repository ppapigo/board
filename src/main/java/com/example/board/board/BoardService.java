package com.example.board.board;

import com.example.board.auth.UserRepository;
import com.example.board.board.dto.BoardDTO;
import com.example.board.board.dto.BoardRequest;
import com.example.board.board.dto.BoardResponse;
import com.example.board.global.entity.Board;
import com.example.board.global.entity.User;
import com.example.board.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    public void validateUser(Long loginUserId){
        User user = new User();
        BusinessException businessException = null;
        user = userRepository.findById(loginUserId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));

        if(user.getRole() != User.Role.ADMIN){
            throw new ForbiddenException(ErrorCode.BOARD_ACCESS_DENIED);
        }
    }
    // Create
    @Transactional
    public BoardResponse create(Long loginUserId, BoardRequest request){
        validateUser(loginUserId);

        if(boardRepository.existsByName(request.getName())){
            // return error
            throw new DuplicateUserException(ErrorCode.DUPLICATE_BOARD_NAME);


        }
        Board board = new Board();
        board.setName(request.getName());
        board.setDescription(request.getDescription());


        Board savedBoard = boardRepository.save( board );

        BoardResponse response = new BoardResponse();
        response.setId(savedBoard.getId());
        response.setName(savedBoard.getName());
        response.setDescription(savedBoard.getDescription());
        response.setCreatedAt(savedBoard.getCreatedAt().toString());

        return response;
    }

    //Read
    @Transactional(readOnly = true)
    public List<BoardResponse> list(){
        return boardRepository.findAll().stream().map(Board::toDTO).toList();
    }
    //Update
    @Transactional
    public BoardResponse update(Long loginUserId, Long boardId, BoardRequest request){
        validateUser(loginUserId);

        Board board = boardRepository.findById(boardId)
                .orElseThrow(()->new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

       board.setName(request.getName());
       board.setDescription(request.getDescription());
        Board savedBoard = boardRepository.save( board );
        return Board.toDTO(savedBoard);
    }

    //Delete
    @Transactional
    public String delete(Long loginUserId, Long boardId){
        validateUser(loginUserId);

        Board board = boardRepository.findById(boardId)
                .orElseThrow(()->new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

        boardRepository.delete(board);

        return "ok";
    }
}
