package tn.esprit.backend_esprittech.dto.sujetprojet.response;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SujetProjetResponseTest {

    @Test
    void recordAccessors_returnAllFields() {
        LocalDateTime now = LocalDateTime.now();
        SujetProjetResponse response = new SujetProjetResponse(
                1L, "Titre", CategorieSujet.PFE, "Desc", "Obj", List.of("Soft skills"), List.of("Domaine"),
                List.of("Java"), 2, StatutSujet.VALIDE, 8.5, false, false, 1001L, "Sami Ben Ali",
                "sami@esprit.tn", "REEE",                 now, now, null, null, null, null, false, 0, 0
        );

        assertEquals(1L, response.id());
        assertEquals(List.of("Soft skills"), response.prerequis());
        assertEquals(List.of("Domaine"), response.domaines());
        assertEquals("Sami Ben Ali", response.encadrantNom());
        assertEquals(List.of("Java"), response.technologies());
    }
}
