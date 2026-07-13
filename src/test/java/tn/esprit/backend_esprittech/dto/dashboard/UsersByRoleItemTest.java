package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.dashboard.extended.UsersByRoleItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsersByRoleItemTest {

    @Test
    void recordAccessors_returnValues() {
        UsersByRoleItem item = new UsersByRoleItem("Étudiant", 30L);

        assertEquals("Étudiant", item.role());
        assertEquals(30L, item.value());
    }

    @Test
    void record_toString_containsFields() {
        UsersByRoleItem item = new UsersByRoleItem("Enseignant", 15L);

        String str = item.toString();
        assertEquals(true, str.contains("Enseignant"));
        assertEquals(true, str.contains("15"));
    }
}
