package com.example.board.auth;

import ch.qos.logback.core.joran.util.beans.BeanDescriptionFactory;
import com.example.board.auth.dto.*;
import com.example.board.global.IngestResult;
import com.example.board.global.exception.ErrorCode;
import com.example.board.global.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    public static final String LOGIN_USER_ID = "LoginUserId";

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Value(("${jwt.refresh-token-validity-seconds}"))
    private long refreshTokenExpiredSec;

    @PostMapping("/signup")
    public ResponseEntity<IngestResult> signup(
            @Valid
            @RequestBody
            SignupRequest request){
        IngestResult result = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid
            @RequestBody
            LoginRequest request
            ){

        UserResponse response = authService.login(request);

       ResponseCookie cookie = refreshCookieFactory
               .create(response.getRefreshToken(), refreshTokenExpiredSec);


        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);

    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false)String refreshToken){
        if(refreshToken != null) {
            authService.logout(refreshToken);
        }
        ResponseCookie cookie = refreshCookieFactory.expire();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/reissue")
    public TokenResponse reissue(@CookieValue(name = "refreshToken", required = false)String refreshToken){

        if( refreshToken == null){
            throw new UnauthorizedException(ErrorCode.LOGIN_REQUIRED);
        }
        log.debug("refresh token in cookie: {}",refreshToken);

        return authService.reissueToken(refreshToken);
    }
}

// JWT : JSON Web Token
// Refresh Token, Access Token

// Redis(DB, NoSQL, Cache) -> 메모리 기반 데이터베이스

// SQL 기반의 DB