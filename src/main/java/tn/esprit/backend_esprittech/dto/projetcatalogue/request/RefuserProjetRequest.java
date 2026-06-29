package tn.esprit.backend_esprittech.dto.projetcatalogue.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motif obligatoire fourni par le chef d'équipe lorsqu'il refuse un projet
 * (le projet passe alors au statut Invalide).
 */
public record RefuserProjetRequest(

        @NotBlank(message = "Le motif de refus est obligatoire")
        @Size(min = 5, message = "Le motif de refus doit contenir au moins 5 caractères")
        String motifRefus
) {
}
