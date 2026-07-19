package com.sbs.board.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    DUPLICATE_USER_EMAIL(HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."),
    DUPLICATE_BOARD_NAME(HttpStatus.CONFLICT, "이미 존재하는 게시판입니다."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시글을 작성할 권한이 없습니다."),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시판을 생성할 권한이 없습니다."),

    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인에 실패하였습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_BOARD_ID(HttpStatus.BAD_REQUEST, "해당 Board ID는 삭제할 수 없습니다."),
    INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "정상적인 인증 요청이 아닙니다."),

    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST,"지원하지 않는 파일 형식입니다"),
    INVALID_FILE_UPLOAD_DIR(HttpStatus.INTERNAL_SERVER_ERROR,"파일 업로드 디렉토리 실패"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"파일 업로드 실패"),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST,"파일 업로드 개수 초과"),
    MAX_UPLOAD_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST,"파일 용량 초과"),


    SQL_INTEGRITY_ERROR(HttpStatus.BAD_REQUEST, "데이터베이스 참조 무결성 위배 에러입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 내부 에러가 발생했습니다.");

    private final HttpStatus status;
    private final String message;


}
