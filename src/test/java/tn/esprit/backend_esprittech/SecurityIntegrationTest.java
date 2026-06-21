package tn.esprit.backend_esprittech;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.entity.ReferentielUtilisateur;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.ReferentielUtilisateurRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "SERVER_PORT=0",
        "JWT_SECRET=integration-test-secret-key-at-least-32-bytes-long-for-hmac",
        "JWT_ACCESS_EXPIRATION=900000",
        "JWT_REFRESH_EXPIRATION=604800000",
        "ADMIN_EMAIL=admin@test.tn",
        "ADMIN_PASSWORD=TestAdmin123",
        "CORS_ALLOWED_ORIGINS=http://localhost:4200",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private ReferentielUtilisateurRepository referentielRepository;

    /** Alimente le référentiel (source de vérité) de façon idempotente avant chaque test. */
    @BeforeEach
    void seedReferentiel() {
        // L'email doit se terminer par @esprit.tn (contrainte du RegisterRequest)
        seed("new@esprit.tn", "ETU-999", TypeUtilisateur.ETUDIANT);
        seed("dup@esprit.tn", "ETU-DUP", TypeUtilisateur.ETUDIANT);
    }

    private void seed(String email, String identifiant, TypeUtilisateur type) {
        if (!referentielRepository.existsByEmail(email)) {
            referentielRepository.save(ReferentielUtilisateur.builder()
                    .nom("Seed").prenom("User")
                    .email(email).identifiant(identifiant)
                    .type(type)
                    .build());
        }
    }

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
                        .content("{\"nom\":\"New\",\"prenom\":\"User\",\"email\":\"new@esprit.tn\",\"identifiant\":\"ETU-999\",\"password\":\"Password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String body = "{\"nom\":\"Dup\",\"prenom\":\"User\",\"email\":\"dup@esprit.tn\","
                + "\"identifiant\":\"ETU-DUP\",\"password\":\"Password123\"}";

        // 1er enregistrement : le compte est créé
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 2e enregistrement avec le même email : conflit
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
