package tn.esprit.backend_esprittech.entity.sujetprojet;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SujetProjetTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private SujetProjet validSujet() {
        return SujetProjet.builder()
                .titre("Sujet IA")
                .categorie(CategorieSujet.PFE)
                .description("Description du sujet")
                .objectifs("Objectif principal")
                .prerequis("Java, Spring")
                .domaine("Intelligence Artificielle")
                .technologies("Java,Spring Boot")
                .capaciteAccueil(2)
                .statut(StatutSujet.SOUMIS_EN_VALIDATION)
                .encadrantId(1001L)
                .build();
    }

    @Test
    void validEntity_hasNoViolations() {
        Set<ConstraintViolation<SujetProjet>> violations = validator.validate(validSujet());
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankTitre_failsValidation() {
        SujetProjet sujet = validSujet();
        sujet.setTitre("  ");

        Set<ConstraintViolation<SujetProjet>> violations = validator.validate(sujet);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("titre")));
    }

    @Test
    void titreTooLong_failsValidation() {
        SujetProjet sujet = validSujet();
        sujet.setTitre("x".repeat(256));

        Set<ConstraintViolation<SujetProjet>> violations = validator.validate(sujet);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("titre")));
    }

    @Test
    void nullCategorie_failsValidation() {
        SujetProjet sujet = validSujet();
        sujet.setCategorie(null);

        Set<ConstraintViolation<SujetProjet>> violations = validator.validate(sujet);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("categorie")));
    }

    @Test
    void capaciteAccueilOutOfRange_failsValidation() {
        SujetProjet sujet = validSujet();
        sujet.setCapaciteAccueil(0);

        Set<ConstraintViolation<SujetProjet>> violations = validator.validate(sujet);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("capaciteAccueil")));
    }

    @Test
    void negativeEncadrantId_failsValidation() {
        SujetProjet sujet = validSujet();
        sujet.setEncadrantId(-1L);

        Set<ConstraintViolation<SujetProjet>> violations = validator.validate(sujet);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("encadrantId")));
    }

    @Test
    void onCreate_setsDatesWhenNull() {
        SujetProjet sujet = validSujet();
        sujet.setDateCreation(null);
        sujet.setDateSoumission(null);

        sujet.onCreate();

        assertNotNull(sujet.getDateCreation());
        assertNotNull(sujet.getDateSoumission());
    }

    @Test
    void onCreate_keepsExistingDates() {
        SujetProjet sujet = validSujet();
        LocalDateTime creation = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime soumission = LocalDateTime.of(2024, 1, 2, 10, 0);
        sujet.setDateCreation(creation);
        sujet.setDateSoumission(soumission);

        sujet.onCreate();

        assertEquals(creation, sujet.getDateCreation());
        assertEquals(soumission, sujet.getDateSoumission());
    }

    @Test
    void builder_defaultFlags() {
        SujetProjet sujet = SujetProjet.builder()
                .titre("Test")
                .categorie(CategorieSujet.STAGE_INGENIEUR)
                .description("Desc")
                .objectifs("Obj")
                .prerequis("Pre")
                .domaine("Domaine")
                .technologies("Java")
                .capaciteAccueil(1)
                .statut(StatutSujet.EN_ATTENTE)
                .encadrantId(1L)
                .build();

        assertFalse(sujet.getEligibleIndustrialisation());
        assertFalse(sujet.getCatalogue());
    }

    @Test
    void allArgsConstructor_andGettersSetters() {
        LocalDateTime now = LocalDateTime.now();
        SujetProjet sujet = new SujetProjet(
                1L, "Titre", CategorieSujet.RDI, "Desc", "Obj", "Pre", "Domaine",
                "Java", 3, StatutSujet.VALIDE, 9.5, true, true, 1001L,
                now, now, now, now, now, "Motif"
        );

        assertEquals(1L, sujet.getId());
        assertEquals("Titre", sujet.getTitre());
        assertEquals(CategorieSujet.RDI, sujet.getCategorie());
        assertEquals(9.5, sujet.getScoreFinal());
        assertTrue(sujet.getEligibleIndustrialisation());
        assertEquals("Motif", sujet.getMotifInvalidation());

        sujet.setScoreFinal(10.0);
        assertEquals(10.0, sujet.getScoreFinal());
    }
}
