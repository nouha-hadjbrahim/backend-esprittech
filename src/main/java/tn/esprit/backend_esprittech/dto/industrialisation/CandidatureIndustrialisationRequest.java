package tn.esprit.backend_esprittech.dto.industrialisation;

import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;

public record CandidatureIndustrialisationRequest(
        @NotNull TypeIndustrialisation typeIndustrialisation,
        String commentaire,
        Boolean confirmEliminatoryWarnings
) {
    public CandidatureIndustrialisationRequest(TypeIndustrialisation typeIndustrialisation, String commentaire) {
        this(typeIndustrialisation, commentaire, false);
    }

    public boolean isConfirmed() {
        return Boolean.TRUE.equals(confirmEliminatoryWarnings);
    }
}
