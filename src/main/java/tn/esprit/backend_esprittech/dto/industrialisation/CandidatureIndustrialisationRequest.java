package tn.esprit.backend_esprittech.dto.industrialisation;

import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;

public record CandidatureIndustrialisationRequest(
        @NotNull TypeIndustrialisation typeIndustrialisation,
        String commentaire
) {
}
