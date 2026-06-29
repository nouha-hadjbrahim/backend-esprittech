package tn.esprit.backend_esprittech.dto.equipe;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TraiterAffiliationRequestTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_hasNoViolations() {
        TraiterAffiliationRequest request = new TraiterAffiliationRequest(StatutAffiliation.ACCEPTEE, "Motif");
        Set<ConstraintViolation<TraiterAffiliationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullStatut_failsValidation() {
        TraiterAffiliationRequest request = new TraiterAffiliationRequest(null, "Motif");
        Set<ConstraintViolation<TraiterAffiliationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void request_withNullMotif_isValid() {
        TraiterAffiliationRequest request = new TraiterAffiliationRequest(StatutAffiliation.REFUSEE, null);
        Set<ConstraintViolation<TraiterAffiliationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void recordAccessors_returnValues() {
        TraiterAffiliationRequest request = new TraiterAffiliationRequest(StatutAffiliation.ACCEPTEE, "Bienvenue");
        assertEquals(StatutAffiliation.ACCEPTEE, request.statut());
        assertEquals("Bienvenue", request.motifDecision());
    }
}
