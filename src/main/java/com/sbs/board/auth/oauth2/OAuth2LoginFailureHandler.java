package com.sbs.board.auth.oauth2;

import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.OAuth2DuplicateEmailException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        ErrorCode errorCode = ErrorCode.LOGIN_FAILED;

        if(exception instanceof OAuth2DuplicateEmailException){
            errorCode = ErrorCode.DUPLICATE_USER_EMAIL;
        }
        response.sendRedirect("/login?oauthError=" + errorCode.name());
    }
}
