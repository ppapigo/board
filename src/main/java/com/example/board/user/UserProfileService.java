package com.example.board.user;

import com.example.board.auth.UserRepository;
import com.example.board.global.entity.User;
import com.example.board.global.entity.UserProfile;
import com.example.board.global.exception.ErrorCode;
import com.example.board.global.exception.UnauthorizedException;
import com.example.board.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileResponse me(Long loginUserId) {
        User user = userRepository.findById(loginUserId)
                .orElseThrow(()->new UnauthorizedException(ErrorCode.LOGIN_REQUIRED));

        UserProfile userProfile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.USER_NOT_FOUND));

        return UserProfile.toDTO(userProfile);
    }
}
