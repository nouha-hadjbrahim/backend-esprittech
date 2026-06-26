package tn.esprit.backend_esprittech.dto.livrable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;

public record LivrableUpdateRequest(
        @NotNull TypeLivrable typeLivrable,
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 2000) String description,
        @Size(max = 1000) String lienExterne,
        Boolean actif
) {
}
