package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.dashboard.extended.SujetByStatutItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SujetByStatutItemTest {

    @Test
    void recordAccessors_returnValues() {
        SujetByStatutItem item = new SujetByStatutItem("En attente", 5L, "#F59E0B");

        assertEquals("En attente", item.name());
        assertEquals(5L, item.value());
        assertEquals("#F59E0B", item.color());
    }

    @Test
    void record_toString_containsFields() {
        SujetByStatutItem item = new SujetByStatutItem("Validés", 10L, "#22C55E");

        String str = item.toString();
        assertEquals(true, str.contains("Validés"));
        assertEquals(true, str.contains("10"));
    }
}
