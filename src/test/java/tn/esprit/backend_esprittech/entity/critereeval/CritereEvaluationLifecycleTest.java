package tn.esprit.backend_esprittech.entity.critereeval;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CritereEvaluationLifecycleTest {

    @Test
    void prePersist_shouldSetDatesAndDefaultFlagsWhenMissing() {
        CritereNote critere = new CritereNote();
        critere.setActif(null);
        critere.setRuleEnabled(null);

        critere.prePersist();

        assertNotNull(critere.getDateCreation());
        assertNotNull(critere.getDateMiseAJour());
        assertTrue(critere.getActif());
        assertFalse(critere.getRuleEnabled());
    }

    @Test
    void prePersist_shouldPreserveExistingFlags() {
        CritereNote critere = new CritereNote();
        critere.setActif(false);
        critere.setRuleEnabled(true);

        critere.prePersist();

        assertFalse(critere.getActif());
        assertTrue(critere.getRuleEnabled());
    }

    @Test
    void preUpdate_shouldRefreshUpdateDate() {
        CritereNote critere = new CritereNote();

        critere.preUpdate();

        LocalDateTime updated = critere.getDateMiseAJour();
        assertNotNull(updated);
    }
}
