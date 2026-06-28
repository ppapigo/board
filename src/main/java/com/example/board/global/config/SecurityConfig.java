package com.example.board.global.config;

import com.example.board.auth.jwt.JwtAuthenticationFilter;
import com.example.board.global.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @EnableMethodSecurity: @PreAuthorize / @PostAuthorize가 활성화됨

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder(){
        // 평문을 암호화하는 객체,암호화만 가능
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();

    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.csrf(AbstractHttpConfigurer::disable)      //CSRF 비활성화
                .formLogin(AbstractHttpConfigurer::disable)  //Spring Security Form Login 기능 사용 x
                .httpBasic(AbstractHttpConfigurer::disable)  //Spring Security Form Login 기능 사용 x
                .sessionManagement(session->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 처리

                .authorizeHttpRequests(auth->auth
                        //공개: 인증/회원가입/로그아웃/게시판의 게시글 조회
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/board/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/post/**").permitAll()
                        //인증이되어야 사용가능
                        .requestMatchers(HttpMethod.GET,"/api/user/me").authenticated()
                        //Admin 권한이 있어야 이용가능
                        /*.requestMatchers(HttpMethod.POST,"/api/board/*").hasRole(User.Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT,"/api/board/**").hasRole(User.Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE,"/api/board/**").hasRole(User.Role.ADMIN.name())*/
                        .anyRequest().authenticated()
                )
                // 401(Unauthorized), 403(forbidden)
                .exceptionHandling(e ->e
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
}
