package com.example.board.user;

import com.example.board.auth.AuthController;
import com.example.board.auth.AuthService;
import com.example.board.auth.LoginUserId;
import com.example.board.auth.dto.LoginRequest;
import com.example.board.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public UserProfileResponse me(@LoginUserId Long loginUserId){

     return userProfileService.me(loginUserId);
    }
}
