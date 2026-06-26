package tn.esprit.backend_esprittech.dto.sujetprojet.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SujetProjetRequestTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private SujetProjetRequest validRequest() {
        return new SujetProjetRequest(
                "Sujet de test",
                CategorieSujet.PFE,
                "Description complète",
                "Objectifs du projet",
                List.of("Soft skills"),
                List.of("Informatique"),
                List.of("Java", "Spring Boot"),
                2
        );
    }

    @Test
    void validRequest_hasNoViolations() {
        Set<ConstraintViolation<SujetProjetRequest>> violations = validator.validate(validRequest());
        assertTrue(violations.isEmpty());
    }

    @Test
    void blankTitre_failsValidation() {
        SujetProjetRequest request = new SujetProjetRequest(
                "", CategorieSujet.PFE, "Desc", "Obj", List.of("Pre"), List.of("Domaine"), List.of("Java"), 1
        );

        Set<ConstraintViolation<SujetProjetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("titre")));
    }

    @Test
    void nullCategorie_failsValidation() {
        SujetProjetRequest request = new SujetProjetRequest(
                "Titre", null, "Desc", "Obj", List.of("Pre"), List.of("Domaine"), List.of("Java"), 1
        );

        Set<ConstraintViolation<SujetProjetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("categorie")));
    }

    @Test
    void emptyTechnologies_failsValidation() {
        SujetProjetRequest request = new SujetProjetRequest(
                "Titre", CategorieSujet.PFE, "Desc", "Obj", List.of("Pre"), List.of("Domaine"), Collections.emptyList(), 1
        );

        Set<ConstraintViolation<SujetProjetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("technologies")));
    }

    @Test
    void capaciteAccueilTooHigh_failsValidation() {
        SujetProjetRequest request = new SujetProjetRequest(
                "Titre", CategorieSujet.PFE, "Desc", "Obj", List.of("Pre"), List.of("Domaine"), List.of("Java"), 51
        );

        Set<ConstraintViolation<SujetProjetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("capaciteAccueil")));
    }

    @Test
    void emptyDomaines_failsValidation() {
        SujetProjetRequest request = new SujetProjetRequest(
                "Titre", CategorieSujet.PFE, "Desc", "Obj", List.of("Pre"), Collections.emptyList(), List.of("Java"), 1
        );

        Set<ConstraintViolation<SujetProjetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("domaines")));
    }

    @Test
    void domaineNameTooLong_failsValidation() {
        SujetProjetRequest request = new SujetProjetRequest(
                "Titre", CategorieSujet.PFE, "Desc", "Obj", List.of("Pre"), List.of("x".repeat(151)), List.of("Java"), 1
        );

        Set<ConstraintViolation<SujetProjetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().startsWith("domaines")));
    }

    @Test
    void recordAccessors_returnValues() {
        SujetProjetRequest request = validRequest();

        assertEquals("Sujet de test", request.titre());
        assertEquals(CategorieSujet.PFE, request.categorie());
        assertEquals(2, request.capaciteAccueil());
        assertEquals(2, request.technologies().size());
    }
}
