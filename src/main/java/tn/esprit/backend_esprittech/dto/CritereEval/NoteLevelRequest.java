package tn.esprit.backend_esprittech.dto.CritereEval;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoteLevelRequest(
        @NotNull @Min(1) Integer value,
        @NotBlank String label,
        String description,
        Boolean active,
        @NotNull @Min(1) Integer order
) {
}
