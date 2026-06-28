package tn.esprit.backend_esprittech.dto.equipe;

import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;

public record TraiterAffiliationRequest(
        @NotNull(message = "Le statut de décision est obligatoire")
        StatutAffiliation statut,

        String motifDecision
) {
}
