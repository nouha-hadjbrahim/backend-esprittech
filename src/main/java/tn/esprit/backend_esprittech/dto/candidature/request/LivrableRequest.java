package tn.esprit.backend_esprittech.dto.candidature.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.enums.TypeLivrable;

public record LivrableRequest(
        @NotNull(message = "Le type de livrable est obligatoire")
        TypeLivrable type,

        @NotBlank(message = "Le nom du livrable est obligatoire")
        String nom,

        @NotBlank(message = "Le lien ou chemin vers le livrable est obligatoire")
        String lien,

        String description,

        boolean visible
) {}
