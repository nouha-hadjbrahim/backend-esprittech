package tn.esprit.backend_esprittech.dto.critereeval;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.critereeval.RuleOperator;

public record CritereNoteRuleRequest(
        @NotBlank String ruleName,
        String description,
        @NotBlank String metadataKey,
        @NotNull RuleOperator operator,
        String expectedValue,
        Double minValue,
        Double maxValue,
        @NotNull @Min(1) Integer noteValue,
        @NotNull @Min(1) Integer priority,
        Boolean active
) {
}
