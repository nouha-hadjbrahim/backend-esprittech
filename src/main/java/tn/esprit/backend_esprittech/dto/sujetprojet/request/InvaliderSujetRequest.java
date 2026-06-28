package tn.esprit.backend_esprittech.dto.sujetprojet.request;

import jakarta.validation.constraints.NotBlank;

/** Motif obligatoire lors de l'invalidation d'un sujet par le chef d'équipe (US-13). */
public record InvaliderSujetRequest(
        @NotBlank(message = "Le motif d'invalidation est obligatoire") String motif
) {
}
