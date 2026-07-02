package tn.esprit.backend_esprittech.dto.CritereEval;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record EvaluationOverrideRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double finalScore,
        String reason
) {
}
