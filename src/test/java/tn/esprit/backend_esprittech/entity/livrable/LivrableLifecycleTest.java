package tn.esprit.backend_esprittech.entity.livrable;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivrableLifecycleTest {

    @Test
    void onCreate_shouldSetMissingDateAndActiveFlag() {
        Livrable livrable = new Livrable();
        livrable.setActif(null);

        livrable.onCreate();

        assertNotNull(livrable.getDateDepot());
        assertTrue(livrable.getActif());
    }

    @Test
    void onCreate_shouldPreserveExistingDateAndActiveFlag() {
        Livrable livrable = new Livrable();
        LocalDateTime fixed = LocalDateTime.of(2026, 1, 1, 9, 0);
        livrable.setDateDepot(fixed);
        livrable.setActif(false);

        livrable.onCreate();

        assertSame(fixed, livrable.getDateDepot());
        assertFalse(livrable.getActif());
    }
}
