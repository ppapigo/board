package com.sbs.board.auth.oauth;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Slf4j
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.oauth.kakao")
public class KakaoOAuthProperties {
    private final String appkey;
    private final String secret;
    private final String callback;
    private final String authorizeUri;
    private final String tokenUri;
    private final String userInfo;

    // 설정 바인딩이 끝난 뒤 필수 Kakao OAuth 값을 검증한다.
    @PostConstruct
    void validate() {
        requireResolved("appkey (KAKAO_REST_API)", appkey);
        requireResolved("secret (KAKAO_SECRET)", secret);
        requireResolved("callback (KAKAO_CALLBACK)", callback);
        requireResolved("authorize-uri", authorizeUri);
        requireResolved("token-uri", tokenUri);
        requireResolved("user-info", userInfo);

        log.info("Kakao OAuth configuration loaded");
    }

    private static void requireResolved(String name, String value) {
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new IllegalStateException("Missing required Kakao OAuth property: " + name);
        }
    }
}
