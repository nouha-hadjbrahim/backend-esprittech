package tn.esprit.backend_esprittech.dto.critereeval;

import tn.esprit.backend_esprittech.entity.critereeval.RuleOperator;

import java.time.LocalDateTime;

public record CritereNoteRuleResponse(
        Long id,
        Long critereNoteId,
        String ruleName,
        String description,
        String metadataKey,
        RuleOperator operator,
        String expectedValue,
        Double minValue,
        Double maxValue,
        Integer noteValue,
        String noteLabel,
        Integer priority,
        Boolean active,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour
) {
}
