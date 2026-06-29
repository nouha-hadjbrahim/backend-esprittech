package tn.esprit.backend_esprittech.controller.sujetprojet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:sujetprojet_it;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa", "DB_PASSWORD=", "SERVER_PORT=0",
        "JWT_SECRET=integration-test-secret-key-at-least-32-bytes-long-for-hmac",
        "JWT_ACCESS_EXPIRATION=900000", "JWT_REFRESH_EXPIRATION=604800000",
        "ADMIN_EMAIL=admin@test.tn", "ADMIN_PASSWORD=TestAdmin123",
        "CORS_ALLOWED_ORIGINS=http://localhost:4200",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SujetProjetIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private String enseignantToken;

    @BeforeEach
    void seedEnseignant() {
        if (userRepository.findByEmail("enseignant@esprit.tn").isEmpty()) {
            userRepository.save(User.builder()
                    .nom("Ben Ali").prenom("Sami").email("enseignant@esprit.tn")
                    .identifiant("ENS-100").password(passwordEncoder.encode("Password123"))
                    .role(Role.ROLE_ENSEIGNANT).typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                    .departement("Informatique").enabled(true).build());
        }
        User enseignant = userRepository.findByEmail("enseignant@esprit.tn").orElseThrow();
        enseignantToken = tokenProvider.generateAccessToken(enseignant);
    }

    @Test
    void endpoints_withoutAuth_return401() throws Exception {
        mockMvc.perform(get("/api/sujet-projets/technologies")).andExpect(status().isUnauthorized());
    }

    @Test
    void creerSujet_asEnseignant_persistsSubject() throws Exception {
        Map<String, Object> payload = Map.of(
                "titre", "Sujet intégration", "categorie", "PFE",
                "description", "Description intégration", "objectifs", "Objectif 1",
                "prerequis", List.of("Soft skills"), "domaines", List.of("Informatique"),
                "technologies", List.of("Java", "Spring Boot"), "capaciteAccueil", 2
        );

        mockMvc.perform(post("/api/sujet-projets")
                        .with(csrf())
                        .cookie(new Cookie("access_token", enseignantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("SOUMIS_EN_VALIDATION"))
                .andExpect(jsonPath("$.encadrantNom").value("Sami Ben Ali"));

        mockMvc.perform(get("/api/sujet-projets/mes-sujets")
                        .cookie(new Cookie("access_token", enseignantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titre").value("Sujet intégration"));
    }
}
