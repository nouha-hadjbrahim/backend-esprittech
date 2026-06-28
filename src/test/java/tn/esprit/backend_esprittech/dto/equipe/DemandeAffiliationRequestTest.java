package tn.esprit.backend_esprittech.dto.equipe;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DemandeAffiliationRequestTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_hasNoViolations() {
        DemandeAffiliationRequest request = new DemandeAffiliationRequest(1L);
        Set<ConstraintViolation<DemandeAffiliationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullEquipeId_failsValidation() {
        DemandeAffiliationRequest request = new DemandeAffiliationRequest(null);
        Set<ConstraintViolation<DemandeAffiliationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void recordAccessors_returnValues() {
        DemandeAffiliationRequest request = new DemandeAffiliationRequest(5L);
        assertEquals(5L, request.equipeId());
    }
}
