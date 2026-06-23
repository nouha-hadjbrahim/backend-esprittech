package tn.esprit.backend_esprittech.mapper.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.sujetprojet.MockEncadrantContext;

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
                "  Titre sujet  ",
                CategorieSujet.PFE,
                "  Description  ",
                "  Objectifs  ",
                "  Prérequis  ",
                "  Domaine  ",
                List.of("Java", "Spring Boot"),
                3
        );
    }

    @Test
    void toEntity_mapsAndTrimsFields() {
        SujetProjet entity = mapper.toEntity(validRequest());

        assertEquals("Titre sujet", entity.getTitre());
        assertEquals(CategorieSujet.PFE, entity.getCategorie());
        assertEquals("Description", entity.getDescription());
        assertEquals("Objectifs", entity.getObjectifs());
        assertEquals("Prérequis", entity.getPrerequis());
        assertEquals("Domaine", entity.getDomaine());
        assertEquals("Java,Spring Boot", entity.getTechnologies());
        assertEquals(3, entity.getCapaciteAccueil());
        assertEquals(StatutSujet.SOUMIS_EN_VALIDATION, entity.getStatut());
        assertEquals(MockEncadrantContext.ENCADRANT_ID, entity.getEncadrantId());
        assertFalse(entity.getEligibleIndustrialisation());
        assertFalse(entity.getCatalogue());
    }

    @Test
    void updateEntity_updatesFields() {
        SujetProjet entity = SujetProjet.builder()
                .titre("Ancien")
                .categorie(CategorieSujet.STAGE_INGENIEUR)
                .description("Old")
                .objectifs("Old")
                .prerequis("Old")
                .domaine("Old")
                .technologies("Python")
                .capaciteAccueil(1)
                .statut(StatutSujet.EN_ATTENTE)
                .encadrantId(1001L)
                .build();

        mapper.updateEntity(entity, validRequest());

        assertEquals("Titre sujet", entity.getTitre());
        assertEquals(CategorieSujet.PFE, entity.getCategorie());
        assertEquals("Java,Spring Boot", entity.getTechnologies());
        assertEquals(3, entity.getCapaciteAccueil());
        assertEquals(StatutSujet.EN_ATTENTE, entity.getStatut());
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
                .prerequis("Pre")
                .domaine("Domaine")
                .technologies("Java, Python")
                .capaciteAccueil(2)
                .statut(StatutSujet.VALIDE)
                .scoreFinal(9.0)
                .eligibleIndustrialisation(true)
                .catalogue(true)
                .encadrantId(1001L)
                .dateCreation(now)
                .dateSoumission(now)
                .dateValidation(now)
                .dateDebutRealisation(now)
                .dateTerminaison(now)
                .motifInvalidation("Aucun")
                .build();

        SujetProjetResponse response = mapper.toResponse(entity);

        assertEquals(5L, response.id());
        assertEquals("Titre", response.titre());
        assertEquals(List.of("Java", "Python"), response.technologies());
        assertEquals(9.0, response.scoreFinal());
        assertTrue(response.eligibleIndustrialisation());
        assertEquals("Aucun", response.motifInvalidation());
    }

    @Test
    void toResponse_nullTechnologies_returnsEmptyList() {
        SujetProjet entity = SujetProjet.builder()
                .id(1L)
                .titre("T")
                .categorie(CategorieSujet.PFE)
                .description("D")
                .objectifs("O")
                .prerequis("P")
                .domaine("Dom")
                .technologies(null)
                .capaciteAccueil(1)
                .statut(StatutSujet.EN_ATTENTE)
                .encadrantId(1001L)
                .build();

        SujetProjetResponse response = mapper.toResponse(entity);

        assertTrue(response.technologies().isEmpty());
    }

    @Test
    void toResponse_blankTechnologies_returnsEmptyList() {
        SujetProjet entity = SujetProjet.builder()
                .id(1L)
                .titre("T")
                .categorie(CategorieSujet.PFE)
                .description("D")
                .objectifs("O")
                .prerequis("P")
                .domaine("Dom")
                .technologies("   ")
                .capaciteAccueil(1)
                .statut(StatutSujet.EN_ATTENTE)
                .encadrantId(1001L)
                .build();

        assertTrue(mapper.toResponse(entity).technologies().isEmpty());
    }

    @Test
    void toResponse_technologiesWithEmptyEntries_filtersBlanks() {
        SujetProjet entity = SujetProjet.builder()
                .id(1L)
                .titre("T")
                .categorie(CategorieSujet.PFE)
                .description("D")
                .objectifs("O")
                .prerequis("P")
                .domaine("Dom")
                .technologies("Java, , Python, ")
                .capaciteAccueil(1)
                .statut(StatutSujet.EN_ATTENTE)
                .encadrantId(1001L)
                .build();

        assertEquals(List.of("Java", "Python"), mapper.toResponse(entity).technologies());
    }
}
