package com.example.board.auth;

import com.example.board.auth.dto.LoginRequest;
import com.example.board.auth.dto.SignupRequest;
import com.example.board.auth.dto.UserResponse;
import com.example.board.global.entity.User;
import com.example.board.global.entity.UserProfile;
import com.example.board.global.IngestResult;
import com.example.board.global.exception.DuplicateUserException;
import com.example.board.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.board.global.exception.ErrorCode.DUPLICATE_USER_EMAIL;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

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

        // request로부터 주어진 email로 데이터 베이스에서 쿼리하여 UserEntity를 가져온다
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        //패스워드 매칭
       if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
           //비밀번호가 일치하지 않음

       }

       response.setId(user.getId());
       response.setEmail(user.getEmail());
       response.setNickName(user.getNickName());
       response.setRole(user.getRole().toString());

        return response;
    }

    public boolean logout(){
        return true;
    }
}
