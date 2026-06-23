package tn.esprit.backend_esprittech.entity.sujetprojet.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategorieSujetTest {

    @Test
    void values_containsAllCategories() {
        CategorieSujet[] values = CategorieSujet.values();

        assertEquals(4, values.length);
        assertNotNull(CategorieSujet.valueOf("STAGE_INGENIEUR"));
        assertNotNull(CategorieSujet.valueOf("PFE"));
        assertNotNull(CategorieSujet.valueOf("RDI"));
        assertNotNull(CategorieSujet.valueOf("PROJET_COMPLET"));
    }
}
