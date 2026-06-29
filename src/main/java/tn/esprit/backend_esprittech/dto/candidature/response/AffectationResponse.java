package tn.esprit.backend_esprittech.dto.candidature.response;

import tn.esprit.backend_esprittech.enums.StatutAffectation;

import java.time.Instant;

public record AffectationResponse(
        Long id,
        Long sujetId,
        Long etudiantId,
        String etudiantNom,
        String etudiantPrenom,
        StatutAffectation statut,
        Instant dateDebut,
        Instant dateRetrait,
        String motifRetrait
) {}
