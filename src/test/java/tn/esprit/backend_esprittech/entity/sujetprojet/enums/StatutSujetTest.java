package tn.esprit.backend_esprittech.entity.sujetprojet.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatutSujetTest {

    @Test
    void values_containsAllStatuts() {
        StatutSujet[] values = StatutSujet.values();

        assertEquals(12, values.length);
        assertNotNull(StatutSujet.valueOf("SOUMIS_EN_VALIDATION"));
        assertNotNull(StatutSujet.valueOf("INDUSTRIALISE_EXTERNE"));
    }
}
