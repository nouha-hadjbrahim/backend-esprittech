package tn.esprit.backend_esprittech.mapper.candidature;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;
import tn.esprit.backend_esprittech.enums.StatutAffectation;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AffectationMapperTest {

    private final AffectationMapper mapper = new AffectationMapper();

    @Test
    void toResponse_shouldMapAffectationAndStudentFields() {
        Instant dateDebut = Instant.parse("2026-01-01T10:00:00Z");
        Instant dateRetrait = Instant.parse("2026-02-01T10:00:00Z");
        User etudiant = new User();
        etudiant.setId(8L);
        etudiant.setNom("Trabelsi");
        etudiant.setPrenom("Nour");
        Affectation affectation = Affectation.builder()
                .id(200L)
                .sujetId(42L)
                .etudiant(etudiant)
                .statut(StatutAffectation.RETIREE_ARCHIVEE)
                .dateDebut(dateDebut)
                .dateRetrait(dateRetrait)
                .motifRetrait("Abandon")
                .build();

        var response = mapper.toResponse(affectation);

        assertEquals(200L, response.id());
        assertEquals(42L, response.sujetId());
        assertEquals(8L, response.etudiantId());
        assertEquals("Trabelsi", response.etudiantNom());
        assertEquals("Nour", response.etudiantPrenom());
        assertEquals(StatutAffectation.RETIREE_ARCHIVEE, response.statut());
        assertEquals(dateDebut, response.dateDebut());
        assertEquals(dateRetrait, response.dateRetrait());
        assertEquals("Abandon", response.motifRetrait());
    }
}
