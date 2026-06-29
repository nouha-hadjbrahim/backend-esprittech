package tn.esprit.backend_esprittech.dto.equipe;

import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;

import java.time.LocalDateTime;

public record AffiliationEnseignantResponse(
        Long id,
        UserResponse enseignant,
        Long equipeId,
        String equipeNom,
        StatutAffiliation statut,
        LocalDateTime dateDemande,
        LocalDateTime dateDecision,
        String motifDecision
) {
}
