package tn.esprit.backend_esprittech;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=integration-test-secret-key-at-least-32-bytes-long-for-hmac",
        "app.jwt.expiration-ms=900000",
        "app.admin.email=admin@test.tn",
        "app.admin.password=TestAdmin123",
        "app.cors.allowed-origins=http://localhost:4200"
})
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserRepository userRepository;

    @Test
    void login_validCredentials_returnsTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.tn\",\"password\":\"TestAdmin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_badPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.tn\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@test.tn\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_validAccessToken_returns200() throws Exception {
        User admin = userRepository.findByEmail("admin@test.tn").orElseThrow();
        String token = tokenProvider.generateAccessToken(admin);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.tn"));
    }

    @Test
    void me_refreshTokenAsAccess_returns401() throws Exception {
        User admin = userRepository.findByEmail("admin@test.tn").orElseThrow();
        String refreshToken = tokenProvider.generateRefreshToken(admin);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer this-is-not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_nonBearerHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swagger_accessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void register_success_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"New\",\"prenom\":\"User\",\"email\":\"new@test.tn\",\"identifiant\":\"ETU-999\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dup\",\"prenom\":\"User\",\"email\":\"admin@test.tn\",\"identifiant\":\"ETU-DUP\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"prenom\":\"\",\"email\":\"bad\",\"identifiant\":\"\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_validToken_returnsNewTokens() throws Exception {
        User admin = userRepository.findByEmail("admin@test.tn").orElseThrow();
        String refreshToken = tokenProvider.generateRefreshToken(admin);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"not-valid\"}"))
                .andExpect(status().isUnauthorized());
    }
}
