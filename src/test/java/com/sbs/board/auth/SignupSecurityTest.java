package com.sbs.board.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sbs.board.auth.jwt.JwtTokenProvider;
import com.sbs.board.global.entity.User;
import com.sbs.board.user.AdminUserService;
import com.sbs.board.user.UserProfileRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignupSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired AdminUserService adminUserService;
    @Autowired EntityManager entityManager;
    @MockitoBean RedisTokenDenylist tokenDenylist;
    @MockitoBean RedisRefreshTokenStore refreshTokenStore;

    @Test
    void signupWithoutRoleCreatesUserAndProfile() throws Exception {
        signup(signupBody());

        User saved = reloadByEmail("signup@example.com");
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
        assertThat(userProfileRepository.existsByUser(saved)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"USER", "ADMIN", "admin", "ROLE_ADMIN", "SUPERUSER"})
    void suppliedRoleCannotElevateSignup(String role) throws Exception {
        signup(signupBody().put("role", role));

        assertThat(reloadByEmail("signup@example.com").getRole()).isEqualTo(User.Role.USER);
    }

    @Test
    void maliciousSignupCannotUseAdminApisAfterLogin() throws Exception {
        signup(signupBody().put("role", "ADMIN"));
        User saved = reloadByEmail("signup@example.com");

        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"signup@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(response).get("accessToken").asText();

        mvc.perform(put("/api/admin/users/{id}/admin", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, accessToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/board/new")
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized board\"}"))
                .andExpect(status().isForbidden());
        assertThat(reloadByEmail(saved.getEmail()).getRole()).isEqualTo(User.Role.USER);
    }

    @Test
    void anonymousCannotGrantAdmin() throws Exception {
        User target = saveUser("target@example.com", User.Role.USER);

        mvc.perform(put("/api/admin/users/{id}/admin", target.getId()))
                .andExpect(status().isUnauthorized());
        assertThat(reloadByEmail(target.getEmail()).getRole()).isEqualTo(User.Role.USER);
    }

    @Test
    void userCannotGrantAdminToAnotherUser() throws Exception {
        User caller = saveUser("caller@example.com", User.Role.USER);
        User target = saveUser("target@example.com", User.Role.USER);

        mvc.perform(put("/api/admin/users/{id}/admin", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(caller)))
                .andExpect(status().isForbidden());
        assertThat(reloadByEmail(target.getEmail()).getRole()).isEqualTo(User.Role.USER);
    }

    @Test
    void adminCanGrantAdminIdempotently() throws Exception {
        User admin = saveUser("admin@example.com", User.Role.ADMIN);
        User target = saveUser("target@example.com", User.Role.USER);
        String authorization = bearer(admin);

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(put("/api/admin/users/{id}/admin", target.getId())
                            .header(HttpHeaders.AUTHORIZATION, authorization))
                    .andExpect(status().isNoContent());
            assertThat(reloadByEmail(target.getEmail()).getRole()).isEqualTo(User.Role.ADMIN);
        }
    }

    @Test
    void grantingAdminToMissingUserReturnsNotFound() throws Exception {
        User admin = saveUser("admin@example.com", User.Role.ADMIN);

        mvc.perform(put("/api/admin/users/{id}/admin", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void serviceAlsoRejectsNonAdminCallers() {
        User target = saveUser("target@example.com", User.Role.USER);

        assertThatThrownBy(() -> adminUserService.grantAdmin(target.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(target.getRole()).isEqualTo(User.Role.USER);
    }

    private ObjectNode signupBody() {
        return objectMapper.createObjectNode()
                .put("email", "signup@example.com")
                .put("password", "password123")
                .put("nick_name", "New user");
    }

    private void signup(ObjectNode body) throws Exception {
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    private User saveUser(String email, User.Role role) {
        return userRepository.save(User.from(email, "Test user", "unused-password", role));
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createToken(user.getEmail());
    }

    private User reloadByEmail(String email) {
        entityManager.flush();
        entityManager.clear();
        return userRepository.findByEmail(email).orElseThrow();
    }
}
