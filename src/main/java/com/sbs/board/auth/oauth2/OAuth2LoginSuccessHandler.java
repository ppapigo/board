package com.sbs.board.auth.oauth2;

import com.sbs.board.auth.*;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.UnauthorizedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

import static com.sbs.board.global.exception.ErrorCode.LOGIN_REQUIRED;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;

        String provider = oauth2Token.getAuthorizedClientRegistrationId();
        String providerId = provider.toUpperCase(Locale.ROOT) + "_" + authentication.getName();

        User user = userRepository.findByProviderId(providerId)
                .orElseThrow(()-> new UnauthorizedException(ErrorCode.LOGIN_FAILED));


        try {

            com.sbs.board.auth.TokenPair tokenPair = authService.issueRefreshTokenPair(user.getId());

            response.addHeader(HttpHeaders.SET_COOKIE,
                    refreshCookieFactory.create(tokenPair.getToken(), tokenPair.getExpireIn()).toString());
            response.sendRedirect("/oauth/callback");

        } catch (AuthenticationException ex) {
            throw new UnauthorizedException(LOGIN_REQUIRED);
        }
    }
}
// Back-End: http://boardapi.sbs.com/1/new

//CORS : white list, Front-End(http://board.sbs.com)
//CSRF
//XSS
