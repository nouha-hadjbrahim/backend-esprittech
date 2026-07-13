package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.dashboard.extended.TrendItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrendItemTest {

    @Test
    void recordAccessors_returnValues() {
        TrendItem item = new TrendItem("janv. 2026", 42L);

        assertEquals("janv. 2026", item.month());
        assertEquals(42L, item.value());
    }

    @Test
    void record_toString_containsFields() {
        TrendItem item = new TrendItem("févr. 2026", 10L);

        String str = item.toString();
        assertEquals(true, str.contains("févr. 2026"));
        assertEquals(true, str.contains("10"));
    }
}
