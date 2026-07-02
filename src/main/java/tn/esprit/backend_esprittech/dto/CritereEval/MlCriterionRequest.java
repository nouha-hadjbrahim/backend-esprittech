package tn.esprit.backend_esprittech.dto.CritereEval;

public record MlCriterionRequest(
        Long criterionId,
        String type,
        String label,
        String description,
        String domain,
        Double maxScore,
        Double weight,
        Boolean eliminatory
) {
}
