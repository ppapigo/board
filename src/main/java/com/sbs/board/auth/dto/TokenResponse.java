package com.sbs.board.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private Long id;
    private String email;
    private String nickName;
    private String role;
    private String accessToken;
    private String refreshToken;
}
