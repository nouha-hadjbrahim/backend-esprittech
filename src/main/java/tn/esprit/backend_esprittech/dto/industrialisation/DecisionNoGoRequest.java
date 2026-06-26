package tn.esprit.backend_esprittech.dto.industrialisation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DecisionNoGoRequest(
        @NotBlank @Size(max = 2000) String motif
) {
}
