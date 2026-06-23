package tn.esprit.backend_esprittech.controller.sujetprojet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:sujetprojet_it;DB_CLOSE_DELAY=-1",
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
class SujetProjetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sujetProjetEndpoints_areAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/sujet-projets/technologies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/sujet-projets/mock-encadrant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomComplet").value("Dr. Ben Ali Sami"));
    }

    @Test
    void creerSujet_withValidPayload_persistsSubject() throws Exception {
        Map<String, Object> payload = Map.of(
                "titre", "Sujet intégration",
                "categorie", "PFE",
                "description", "Description intégration",
                "objectifs", "Objectif 1",
                "prerequis", "Java",
                "domaine", "Informatique",
                "technologies", List.of("Java", "Spring Boot"),
                "capaciteAccueil", 2
        );

        mockMvc.perform(post("/api/sujet-projets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Sujet intégration"))
                .andExpect(jsonPath("$.statut").value("SOUMIS_EN_VALIDATION"));

        mockMvc.perform(get("/api/sujet-projets/mes-sujets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titre").value("Sujet intégration"));
    }

    @Test
    void creerSujet_withInvalidPayload_returnsValidationError() throws Exception {
        Map<String, Object> payload = Map.of(
                "titre", "",
                "categorie", "PFE",
                "description", "Description",
                "objectifs", "Objectifs",
                "prerequis", "Prérequis",
                "domaine", "Informatique",
                "technologies", List.of(),
                "capaciteAccueil", 0
        );

        mockMvc.perform(post("/api/sujet-projets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation"))
                .andExpect(jsonPath("$.errors").exists());
    }
}
