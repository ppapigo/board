package com.example.board.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.example.board.global.exception.ErrorCode.DUPLICATE_USER_EMAIL;
import static com.example.board.global.exception.ErrorCode.INTERNAL_SERVER_ERROR;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerError(NullPointerException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUserError(DuplicateUserException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(DUPLICATE_USER_EMAIL));
    }
}
