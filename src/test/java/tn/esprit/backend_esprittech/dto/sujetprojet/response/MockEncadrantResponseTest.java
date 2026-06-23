package tn.esprit.backend_esprittech.dto.sujetprojet.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockEncadrantResponseTest {

    @Test
    void recordAccessors_returnAllFields() {
        MockEncadrantResponse response = new MockEncadrantResponse(
                1001L, "Dr. Ben Ali Sami", "Encadrant", "benali.sami@esprittech.tn", "Informatique"
        );

        assertEquals(1001L, response.id());
        assertEquals("Dr. Ben Ali Sami", response.nomComplet());
        assertEquals("Encadrant", response.role());
        assertEquals("benali.sami@esprittech.tn", response.email());
        assertEquals("Informatique", response.departement());
    }
}
