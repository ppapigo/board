package com.example.board.auth;

import com.example.board.auth.dto.LoginRequest;
import com.example.board.auth.dto.SignupRequest;
import com.example.board.auth.dto.UserResponse;
import com.example.board.auth.jwt.JwtTokenProvider;
import com.example.board.global.entity.User;
import com.example.board.global.entity.UserProfile;
import com.example.board.global.IngestResult;
import com.example.board.global.exception.DuplicateUserException;
import com.example.board.global.exception.NotFoundException;
import com.example.board.global.exception.UnauthorizedException;
import com.example.board.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.board.auth.jwt.JwtAuthenticationFilter.BEARER;
import static com.example.board.global.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public IngestResult signUp(SignupRequest request){
        IngestResult result = new IngestResult();

        if(userRepository.existsByEmail(request.getEmail())){
            throw new DuplicateUserException(DUPLICATE_USER_EMAIL);
        }


        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickName(request.getNickName());
        user.setRole(request.getRole().equals("ADMIN") ? User.Role.ADMIN : User.Role.USER);

        User savedUser = userRepository.save(user);

        if(!userProfileRepository.existsByUser(savedUser)){
            //저장된 사용자 인증정보와 매칭되는 프로필 정보도 같이 저장한다.
            UserProfile profile = new UserProfile();
            profile.setUser(savedUser);
            userProfileRepository.save( profile );
        }

        result.setStatus("ok");


        return result;
    }

    @Transactional
    public UserResponse login(LoginRequest request){
        UserResponse response = new UserResponse();

        try{
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.createToken(userDetails.getUsername());

            response.setId(userDetails.getId());
            response.setEmail(userDetails.getUsername());
            response.setNickName(userDetails.getNickName());
           // response.setRole(userDetails.getAuthorities().);
            response.setAccessToken(BEARER+accessToken);
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException(LOGIN_REQUIRED);
        }

       /* // request로부터 주어진 email로 데이터 베이스에서 쿼리하여 UserEntity를 가져온다
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()->new NotFoundException(USER_NOT_FOUND));

        //패스워드 매칭
       if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
           //비밀번호가 일치하지 않음
            throw new UnauthorizedException(LOGIN_FAILED);
       }

       String accessToken = jwtTokenProvider.createToken(user.getEmail());*/




        return response;
    }

    public boolean logout(){
        return true;
    }
}
