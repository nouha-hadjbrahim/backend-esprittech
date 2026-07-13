package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.dashboard.extended.CatalogueByDomainItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogueByDomainItemTest {

    @Test
    void recordAccessors_returnValues() {
        CatalogueByDomainItem item = new CatalogueByDomainItem("Informatique", 4L);

        assertEquals("Informatique", item.domain());
        assertEquals(4L, item.value());
    }

    @Test
    void record_toString_containsFields() {
        CatalogueByDomainItem item = new CatalogueByDomainItem("Réseaux", 7L);

        String str = item.toString();
        assertEquals(true, str.contains("Réseaux"));
        assertEquals(true, str.contains("7"));
    }
}
