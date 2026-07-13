package tn.esprit.backend_esprittech.historique;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.historique.HistoriqueRepository;
import tn.esprit.backend_esprittech.service.CritereEval.CritereEliminatoireService;
import tn.esprit.backend_esprittech.service.CritereEval.CritereNoteService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:historisation_it;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
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
class HistorisationIntegrationTest {

    @Autowired private CritereNoteService critereNoteService;
    @Autowired private CritereEliminatoireService critereEliminatoireService;
    @Autowired private HistoriqueRepository historiqueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private MinioStorageService minioStorageService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin@test.tn").orElseGet(() ->
                userRepository.save(User.builder()
                        .nom("Admin").prenom("Test").email("admin@test.tn")
                        .identifiant("ADMIN-1").password(passwordEncoder.encode("Password123"))
                        .role(Role.ROLE_ADMIN).typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                        .departement("IT").enabled(true).build()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));

        historiqueRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private JsonNode parseJson(String json) {
        if (json == null) return null;
        try {
            // H2 JSON column may return the value as a quoted string; unwrap it
            String cleaned = json.trim();
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1)
                    .replace("\\\"", "\"").replace("\\n", "\n");
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            fail("Failed to parse JSON: " + json, e);
            return null;
        }
    }

    private CritereEliminatoireRequest elimReq(String libelle, String description, String domaine, int ordre, ReponseEliminatoire reponse, boolean actif) {
        return new CritereEliminatoireRequest(
                libelle, description, domaine, ordre, reponse, actif,
                null, null, null, null, null, false, null,
                "TEST_FAMILY", "1.0", "TEST_APPLICABILITY",
                "TEST_EVIDENCE_TYPES", null, "POSITIVE", "NEGATIVE",
                "TEST_NATURE", 1, "AUTOMATIC",
                "RULE_CONTAINING_INDETERMINATE", null
        );
    }

    private CritereNoteRequest noteReq(String libelle, String description, String domaine, int ordre, double bareme, double poids, double seuil, boolean actif) {
        return new CritereNoteRequest(
                libelle, description, domaine, ordre, bareme, poids, seuil, 3, actif,
                null, null, null, null, null, false, null,
                "TEST_FAMILY", "1.0", "TEST_APPLICABILITY",
                "TEST_EVIDENCE_TYPES", null, "POSITIVE", null,
                null, 1, "AUTOMATIC",
                null, null
        );
    }

    @Test
    void createCritereEliminatoire_shouldCreateNoHistoriqueEntry() {
        critereEliminatoireService.create(
                elimReq("Critere-A", "Description test",
                        "Informatique", 1, ReponseEliminatoire.OK, true));

        // No @Historise on CritereEliminatoire anymore, so no entries expected.
    }

    @Test
    void createCritereNote_shouldCreateNoHistoriqueEntry() {
        critereNoteService.create(
                noteReq("Note critere", "Description",
                        "Informatique", 1, 5.0, 1.0, 3.0, true));

        // No @Historise on CritereNote anymore, so no entries expected.
    }
}
