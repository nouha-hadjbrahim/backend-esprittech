package tn.esprit.backend_esprittech.dto.industrialisation;

import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.industrialisation.OrientationIndustrialisation;

public record DecisionGoRequest(
        @NotNull OrientationIndustrialisation orientation,
        String commentaire
) {
}
