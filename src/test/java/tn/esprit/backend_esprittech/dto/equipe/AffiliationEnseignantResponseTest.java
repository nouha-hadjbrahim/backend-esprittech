package tn.esprit.backend_esprittech.dto.equipe;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AffiliationEnseignantResponseTest {

    @Test
    void recordAccessors_returnValues() {
        UserResponse enseignant = new UserResponse(1L, "Doe", "John", "john@test.tn",
                "JD001", Role.ROLE_ENSEIGNANT, TypeUtilisateur.ENSEIGNANT,
                "Informatique", true, Instant.now(), false, null, null);
        LocalDateTime dateDemande = LocalDateTime.now();
        LocalDateTime dateDecision = LocalDateTime.now().plusDays(1);

        AffiliationEnseignantResponse response = new AffiliationEnseignantResponse(
                1L, enseignant, 10L, "Équipe Alpha",
                StatutAffiliation.ACCEPTEE, dateDemande, dateDecision, "Accepté"
        );

        assertEquals(1L, response.id());
        assertEquals(enseignant, response.enseignant());
        assertEquals(10L, response.equipeId());
        assertEquals("Équipe Alpha", response.equipeNom());
        assertEquals(StatutAffiliation.ACCEPTEE, response.statut());
        assertEquals(dateDemande, response.dateDemande());
        assertEquals(dateDecision, response.dateDecision());
        assertEquals("Accepté", response.motifDecision());
    }

    @Test
    void record_toString_containsFields() {
        UserResponse enseignant = new UserResponse(1L, "Doe", "John", "john@test.tn",
                "JD001", Role.ROLE_ENSEIGNANT, TypeUtilisateur.ENSEIGNANT,
                "Informatique", true, Instant.now(), false, null, null);

        AffiliationEnseignantResponse response = new AffiliationEnseignantResponse(
                1L, enseignant, 10L, "Équipe Alpha",
                StatutAffiliation.EN_ATTENTE, LocalDateTime.now(), null, null
        );

        String str = response.toString();
        assertTrue(str.contains("1"));
        assertTrue(str.contains("Équipe Alpha"));
    }
}
