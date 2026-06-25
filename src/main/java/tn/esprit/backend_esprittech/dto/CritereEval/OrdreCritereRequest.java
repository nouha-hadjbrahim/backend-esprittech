package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrdreCritereRequest(
        @NotNull Long critereId,
        @NotNull @Min(1) Integer ordre
) {
}