package tn.esprit.backend_esprittech.mapper.candidature;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.enums.StatutCandidature;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CandidatureMapperTest {

    private final CandidatureMapper mapper = new CandidatureMapper();

    @Test
    void toResponse_shouldMapCandidatureAndStudentFields() {
        Instant dateDepot = Instant.parse("2026-01-01T10:00:00Z");
        Instant dateDecision = Instant.parse("2026-01-02T11:00:00Z");
        User etudiant = new User();
        etudiant.setId(7L);
        etudiant.setNom("Ben Ali");
        etudiant.setPrenom("Sami");
        etudiant.setEmail("sami.benali@esprit.tn");
        Candidature candidature = Candidature.builder()
                .id(100L)
                .sujetId(42L)
                .etudiant(etudiant)
                .statut(StatutCandidature.REFUSEE)
                .motifRefus("Places completes")
                .messageEtudiant("Motivation")
                .dateDepot(dateDepot)
                .dateDecision(dateDecision)
                .build();

        var response = mapper.toResponse(candidature);

        assertEquals(100L, response.id());
        assertEquals(42L, response.sujetId());
        assertEquals(7L, response.etudiantId());
        assertEquals("Ben Ali", response.etudiantNom());
        assertEquals("Sami", response.etudiantPrenom());
        assertEquals("sami.benali@esprit.tn", response.etudiantEmail());
        assertEquals(StatutCandidature.REFUSEE, response.statut());
        assertEquals("Places completes", response.motifRefus());
        assertEquals("Motivation", response.messageEtudiant());
        assertEquals(dateDepot, response.dateDepot());
        assertEquals(dateDecision, response.dateDecision());
    }
}
