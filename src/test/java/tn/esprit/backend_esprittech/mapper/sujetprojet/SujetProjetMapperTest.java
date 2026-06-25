package tn.esprit.backend_esprittech.mapper.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SujetProjetMapperTest {

    private SujetProjetMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SujetProjetMapper();
    }

    private SujetProjetRequest validRequest() {
        return new SujetProjetRequest(
                "  Titre sujet  ", CategorieSujet.PFE, "  Description  ", "  Objectifs  ",
                List.of("Soft skills", "Hard skills"), List.of("  Domaine  "),
                List.of("Java", "Spring Boot"), 3
        );
    }

    @Test
    void toEntity_mapsAndTrimsFields() {
        SujetProjet entity = mapper.toEntity(validRequest(), 42L);

        assertEquals("Titre sujet", entity.getTitre());
        assertEquals("Soft skills,Hard skills", entity.getPrerequis());
        assertEquals("Domaine", entity.getDomaine());
        assertEquals(42L, entity.getEncadrantId());
        assertEquals(StatutSujet.SOUMIS_EN_VALIDATION, entity.getStatut());
    }

    @Test
    void toResponse_mapsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        SujetProjet entity = SujetProjet.builder()
                .id(5L)
                .titre("Titre")
                .categorie(CategorieSujet.RDI)
                .description("Desc")
                .objectifs("Obj")
                .prerequis("Soft skills,Java")
                .domaine("Domaine")
                .technologies("Java, Python")
                .capaciteAccueil(2)
                .statut(StatutSujet.VALIDE)
                .scoreFinalEvaluation(9.0)
                .eligibleIndustrialisation(true)
                .catalogue(true)
                .encadrantId(1001L)
                .dateCreation(now)
                .build();

        SujetProjetResponse response = mapper.toResponse(entity, "Dr. Sami");

        assertEquals(List.of("Soft skills", "Java"), response.prerequis());
        assertEquals(List.of("Domaine"), response.domaines());
        assertEquals("Dr. Sami", response.encadrantNom());
    }
}
