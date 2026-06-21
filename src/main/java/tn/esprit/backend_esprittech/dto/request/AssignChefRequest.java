package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Assignation (ou changement) du chef d'une équipe existante.
 */
public record AssignChefRequest(

        @NotNull(message = "L'identifiant du chef est obligatoire")
        Long chefId
) {
}
