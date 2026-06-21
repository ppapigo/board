package com.example.board.auth;

import com.example.board.auth.dto.LoginRequest;
import com.example.board.auth.dto.SignupRequest;
import com.example.board.auth.dto.UserResponse;
import com.example.board.global.IngestResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    public static final String LOGIN_USER_ID = "LoginUserId";

    private final AuthService authService;

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



        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request){


    }
}

// JWT : JSON Web Token
// Refresh Token, Access Token

// Redis(DB, NoSQL, Cache) -> 메모리 기반 데이터베이스

// SQL 기반의 DB