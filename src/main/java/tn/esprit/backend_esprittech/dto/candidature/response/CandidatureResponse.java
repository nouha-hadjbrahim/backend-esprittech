package tn.esprit.backend_esprittech.dto.candidature.response;

import tn.esprit.backend_esprittech.enums.StatutCandidature;

import java.time.Instant;

public record CandidatureResponse(
        Long id,
        Long sujetId,
        Long etudiantId,
        String etudiantNom,
        String etudiantPrenom,
        String etudiantEmail,
        StatutCandidature statut,
        String motifRefus,
        String messageEtudiant,
        Instant dateDepot,
        Instant dateDecision,
        String sujetTitre,
        String sujetCategorie,
        String encadrantNom
) {}
