package tn.esprit.backend_esprittech;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.service.AzureGraphService;
import tn.esprit.backend_esprittech.service.GraphMailService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-integration-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "server.port=0",
        "app.jwt.secret=integration-test-secret-key-at-least-32-bytes-long-for-hmac",
        "app.jwt.expiration-ms=900000",
        "app.jwt.refresh-expiration-ms=604800000",
        "app.admin.email=admin@test.tn",
        "app.admin.password=TestAdmin123",
        "app.auth.cookie.secure=false",
        "app.auth.cookie.same-site=Strict",
        "app.cors.allowed-origins=http://localhost:4200",
        "minio.endpoint=http://127.0.0.1:1",
        "minio.access-key=test-access-key",
        "minio.secret-key=test-secret-key",
        "minio.bucket=test-bucket",
        "minio.secure=false"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean private MinioStorageService minioStorageService;
    @MockitoBean private AzureGraphService azureGraphService;
    @MockitoBean private GraphMailService graphMailService;

    /**
     * Simule la vérification Azure : tout email est considéré membre de
     * l'organisation avec le rôle étudiant. L'envoi d'email (Graph) est mocké,
     * donc aucun appel réseau réel n'est effectué pendant les tests.
     */
    @BeforeEach
    void stubAzureGraph() {
        when(azureGraphService.resolveMember(anyString()))
                .thenReturn(new AzureGraphService.GraphMember(TypeUtilisateur.ETUDIANT, "GL"));
    }

    @Test
    void login_validCredentials_setsHttpOnlyCookies_andUserBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.tn\",\"password\":\"TestAdmin123\"}"))
                .andExpect(status().isOk())
                // Le corps ne contient QUE les infos utilisateur, plus aucun token.
                .andExpect(jsonPath("$.email").value("admin@test.tn"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                // Les tokens sont déposés dans des cookies HttpOnly.
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(header().string("Authorization", org.hamcrest.Matchers.startsWith("Bearer ")));
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
    void me_validAccessTokenCookie_returns200() throws Exception {
        User admin = userRepository.findByEmail("admin@test.tn").orElseThrow();
        String token = tokenProvider.generateAccessToken(admin);

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("access_token", token)))
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
    void register_startThenVerify_createsAccountAndSetsCookies() throws Exception {
        // Étape 1 : envoi du code (202, aucun compte encore créé, aucun cookie)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"New\",\"prenom\":\"User\",\"email\":\"new@esprit.tn\",\"password\":\"Password123\"}"))
                .andExpect(status().isAccepted())
                .andExpect(cookie().doesNotExist("access_token"));

        // Le code OTP est capturé depuis l'appel (mocké) au service d'email.
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(graphMailService).sendVerificationCode(eq("new@esprit.tn"), any(), codeCaptor.capture());
        String code = codeCaptor.getValue();

        // Étape 2 : vérification du code → compte créé + cookies
        mockMvc.perform(post("/api/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@esprit.tn\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@esprit.tn"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void register_verifyWrongCode_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Bad\",\"prenom\":\"Code\",\"email\":\"badcode@esprit.tn\",\"password\":\"Password123\"}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/auth/register/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"badcode@esprit.tn\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_existingEmail_returns409() throws Exception {
        // Un compte existe déjà → l'étape 1 est refusée.
        userRepository.save(User.builder()
                .nom("Dup").prenom("User").email("dup@esprit.tn")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true).build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dup\",\"prenom\":\"User\",\"email\":\"dup@esprit.tn\",\"password\":\"Password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"\",\"prenom\":\"\",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_validCookie_setsNewCookies() throws Exception {
        User admin = userRepository.findByEmail("admin@test.tn").orElseThrow();
        String refreshToken = tokenProvider.generateRefreshToken(admin);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.tn"))
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    void refresh_invalidCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "not-valid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_missingCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clearsCookies_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }
}
