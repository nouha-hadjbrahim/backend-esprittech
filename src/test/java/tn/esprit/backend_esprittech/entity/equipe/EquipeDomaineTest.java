package tn.esprit.backend_esprittech.entity.equipe;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EquipeDomaineTest {

    @Test
    void onCreate_dateCreationNull_setsValue() {
        EquipeDomaine domaine = new EquipeDomaine();
        domaine.setDateCreation(null);

        domaine.onCreate();

        assertNotNull(domaine.getDateCreation());
    }

    @Test
    void onCreate_dateCreationAlreadySet_keepsValue() {
        EquipeDomaine domaine = new EquipeDomaine();
        LocalDateTime fixed = LocalDateTime.of(2020, 1, 1, 0, 0);
        domaine.setDateCreation(fixed);

        domaine.onCreate();

        assertEquals(fixed, domaine.getDateCreation());
    }

    @Test
    void builder_setsFields() {
        EquipeDomaine domaine = EquipeDomaine.builder()
                .id(1L)
                .nom("Informatique")
                .build();

        assertEquals(1L, domaine.getId());
        assertEquals("Informatique", domaine.getNom());
    }

    @Test
    void allArgsConstructor_setsFields() {
        LocalDateTime now = LocalDateTime.now();
        EquipeDomaine domaine = new EquipeDomaine(1L, "Informatique", now);

        assertEquals(1L, domaine.getId());
        assertEquals("Informatique", domaine.getNom());
        assertEquals(now, domaine.getDateCreation());
    }
}
