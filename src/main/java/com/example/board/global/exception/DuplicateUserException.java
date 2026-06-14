package com.example.board.global.exception;

public class DuplicateUserException extends BusinessException {
    public DuplicateUserException(ErrorCode errorCode) {

        super(errorCode);
    }
}
