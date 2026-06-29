package tn.esprit.backend_esprittech.dto.livrable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;

public record LivrableLinkRequest(
        @NotNull TypeLivrable typeLivrable,
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 1000) String lienExterne
) {
}
