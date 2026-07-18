package com.sbs.board.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String nickName;
    private String accessToken;
    private String refreshToken;    // production 에서는 제거
    private String role;    // debug용
}
