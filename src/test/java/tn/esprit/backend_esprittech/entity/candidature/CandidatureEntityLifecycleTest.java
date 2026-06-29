package tn.esprit.backend_esprittech.entity.candidature;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CandidatureEntityLifecycleTest {

    @Test
    void candidatureOnDepose_shouldSetMissingDateAndPreserveExistingDate() {
        Candidature missingDate = new Candidature();
        missingDate.onDepose();
        assertNotNull(missingDate.getDateDepot());

        Candidature existingDate = new Candidature();
        Instant fixed = Instant.parse("2026-01-01T10:00:00Z");
        existingDate.setDateDepot(fixed);
        existingDate.onDepose();
        assertSame(fixed, existingDate.getDateDepot());
    }

    @Test
    void affectationOnCreate_shouldSetMissingDateAndPreserveExistingDate() {
        Affectation missingDate = new Affectation();
        missingDate.onCreate();
        assertNotNull(missingDate.getDateDebut());

        Affectation existingDate = new Affectation();
        Instant fixed = Instant.parse("2026-01-01T10:00:00Z");
        existingDate.setDateDebut(fixed);
        existingDate.onCreate();
        assertSame(fixed, existingDate.getDateDebut());
    }
}
