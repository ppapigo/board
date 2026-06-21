package com.example.board.user;

import com.example.board.global.entity.UserProfile;
import com.example.board.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;

    public UserProfileResponse me(Long loginUserId) {
        UserProfile user = userProfileRepository.findById(loginUserId)
                .orElseThrow(() -> new RuntimeException("프로필을 찾을 수 없습니다."));

        return UserProfile.toDTO(user);
    }
}
