package com.example.board.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"사용자를 찾을 수 없습니다"),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND,"게시판을 찾을 수 없습니다"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND,"게시글을 찾을 수 없습니다"),

    DUPLICATE_USER_EMAIL(HttpStatus.CONFLICT,"이미 사용중인 이메일 입니다"),
    DUPLICATE_BOARD_NAME(HttpStatus.CONFLICT,"이미 존재하는 게시판 입니다"),

    POST_ACCEPT_DENIED(HttpStatus.FORBIDDEN,"게시글을 작성할 권한이 없습니다"),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN,"게시판을 생성할 권한이 없습니다"),

    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED,"로그인에 실패하였습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST,"입력값이 올바르지 않습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"알 수 없는 내부 에러가 발생하였습니다");

    private final HttpStatus status;
    private final String message;
}
