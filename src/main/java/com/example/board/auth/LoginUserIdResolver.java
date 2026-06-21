package com.example.board.auth;

import com.example.board.global.exception.ErrorCode;
import com.example.board.global.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.example.board.auth.AuthController.LOGIN_USER_ID;

@Slf4j
@Component
public class LoginUserIdResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        //parameter에 @LoginUserId가 달려있고, 그 타입이 Long이면 True를 반환
        return parameter.hasParameterAnnotation(LoginUserId.class) &&
                parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {
        //들어온 request 추출하기
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        //request로부터 loginUserId 추출하기
        Long loginUserId = request != null ? (Long) request.getAttribute(LOGIN_USER_ID) : null;

        //loginUserId가 없다면?
        if(loginUserId == null){
            //UnauthorizedException 발생하기
            throw new UnauthorizedException(ErrorCode.LOGIN_REQUIRED);
        }

        log.debug("loginUserIdResolver에 의한 사용자 아이디 : {} 추출됨", loginUserId);

        //loginUserId 반환
        return loginUserId;
    }
}
