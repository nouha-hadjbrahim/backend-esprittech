package tn.esprit.backend_esprittech.enums.equipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatutEquipeTest {

    @Test
    void fromValue_null_returnsNull() {
        assertNull(StatutEquipe.fromValue(null));
    }

    @Test
    void fromValue_displayNameActif_returnsACTIF() {
        assertEquals(StatutEquipe.ACTIF, StatutEquipe.fromValue("Actif"));
    }

    @Test
    void fromValue_displayNameInactif_returnsINACTIF() {
        assertEquals(StatutEquipe.INACTIF, StatutEquipe.fromValue("Inactif"));
    }

    @Test
    void fromValue_displayNameCaseInsensitive_returnsACTIF() {
        assertEquals(StatutEquipe.ACTIF, StatutEquipe.fromValue("actif"));
    }

    @Test
    void fromValue_enumNameACTIF_returnsACTIF() {
        assertEquals(StatutEquipe.ACTIF, StatutEquipe.fromValue("ACTIF"));
    }

    @Test
    void fromValue_enumNameINACTIF_returnsINACTIF() {
        assertEquals(StatutEquipe.INACTIF, StatutEquipe.fromValue("INACTIF"));
    }

    @Test
    void fromValue_invalidValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> StatutEquipe.fromValue("INCONNU"));
    }

    @Test
    void getDisplayName_actif_returnsActif() {
        assertEquals("Actif", StatutEquipe.ACTIF.getDisplayName());
    }

    @Test
    void getDisplayName_inactif_returnsInactif() {
        assertEquals("Inactif", StatutEquipe.INACTIF.getDisplayName());
    }
}
