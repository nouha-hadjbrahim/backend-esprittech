package tn.esprit.backend_esprittech.dto.equipe;

import jakarta.validation.constraints.NotBlank;

public record EquipeDomaineRequest(
        @NotBlank(message = "Le nom du domaine est obligatoire")
        String nom
) {
}
