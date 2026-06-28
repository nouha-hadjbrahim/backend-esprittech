package tn.esprit.backend_esprittech.dto.equipe;

import jakarta.validation.constraints.NotNull;

public record DemandeAffiliationRequest(
        @NotNull(message = "L'identifiant de l'équipe est obligatoire")
        Long equipeId
) {
}
