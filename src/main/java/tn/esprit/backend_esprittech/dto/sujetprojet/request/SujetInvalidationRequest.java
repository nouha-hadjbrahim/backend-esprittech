package tn.esprit.backend_esprittech.dto.sujetprojet.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SujetInvalidationRequest(
        @NotBlank(message = "Le motif de refus est obligatoire")
        @Size(max = 2000, message = "Le motif ne doit pas dépasser 2000 caractères")
        String motif
) {
}
