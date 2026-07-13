package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.dashboard.extended.RecentActivityItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentActivityItemTest {

    @Test
    void recordAccessors_returnValues() {
        RecentActivityItem item = new RecentActivityItem(
                1L, "Sujet validé", "Alice Dupont", "À l'instant", "validation"
        );

        assertEquals(1L, item.id());
        assertEquals("Sujet validé", item.message());
        assertEquals("Alice Dupont", item.actor());
        assertEquals("À l'instant", item.time());
        assertEquals("validation", item.type());
    }

    @Test
    void record_toString_containsFields() {
        RecentActivityItem item = new RecentActivityItem(
                5L, "Candidature créée", "Bob Martin", "Il y a 3h", "creation"
        );

        String str = item.toString();
        assertEquals(true, str.contains("Candidature créée"));
        assertEquals(true, str.contains("Bob Martin"));
        assertEquals(true, str.contains("Il y a 3h"));
    }
}
