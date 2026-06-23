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
                1L, "Titre", CategorieSujet.PFE, "Desc", "Obj", "Pre", "Domaine",
                List.of("Java"), 2, StatutSujet.VALIDE, 8.5, false, false, 1001L,
                now, now, null, null, null, null
        );

        assertEquals(1L, response.id());
        assertEquals("Titre", response.titre());
        assertEquals(CategorieSujet.PFE, response.categorie());
        assertEquals(List.of("Java"), response.technologies());
        assertEquals(StatutSujet.VALIDE, response.statut());
        assertEquals(8.5, response.scoreFinal());
        assertEquals(1001L, response.encadrantId());
        assertEquals(now, response.dateCreation());
    }
}
