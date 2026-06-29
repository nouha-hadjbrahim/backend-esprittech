package tn.esprit.backend_esprittech.enums.equipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatutAffiliationTest {

    @Test
    void fromValue_null_returnsNull() {
        assertNull(StatutAffiliation.fromValue(null));
    }

    @Test
    void fromValue_displayNameEnAttente_returnsEN_ATTENTE() {
        assertEquals(StatutAffiliation.EN_ATTENTE, StatutAffiliation.fromValue("En attente"));
    }

    @Test
    void fromValue_displayNameAcceptee_returnsACCEPTEE() {
        assertEquals(StatutAffiliation.ACCEPTEE, StatutAffiliation.fromValue("Acceptée"));
    }

    @Test
    void fromValue_displayNameRefusee_returnsREFUSEE() {
        assertEquals(StatutAffiliation.REFUSEE, StatutAffiliation.fromValue("Refusée"));
    }

    @Test
    void fromValue_displayNameCaseInsensitive_returnsEN_ATTENTE() {
        assertEquals(StatutAffiliation.EN_ATTENTE, StatutAffiliation.fromValue("en attente"));
    }

    @Test
    void fromValue_invalidValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> StatutAffiliation.fromValue("INCONNU"));
    }

    @Test
    void getDisplayName_enAttente_returnsEnAttente() {
        assertEquals("En attente", StatutAffiliation.EN_ATTENTE.getDisplayName());
    }

    @Test
    void getDisplayName_acceptee_returnsAcceptee() {
        assertEquals("Acceptée", StatutAffiliation.ACCEPTEE.getDisplayName());
    }

    @Test
    void getDisplayName_refusee_returnsRefusee() {
        assertEquals("Refusée", StatutAffiliation.REFUSEE.getDisplayName());
    }
}
