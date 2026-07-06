package tn.esprit.backend_esprittech.dto.CritereEval;

import java.util.List;

public record MlCriterionRequest(
        Long criterionId,
        String type,
        String name,
        String label,
        String description,
        String domain,
        Double maximumScore,
        Double maxScore,
        Double weight,
        Boolean eliminatory,
        String version,
        List<String> requiredEvidenceTypes
) {
    public MlCriterionRequest(
            Long criterionId,
            String type,
            String label,
            String description,
            String domain,
            Double maxScore,
            Double weight,
            Boolean eliminatory
    ) {
        this(
                criterionId,
                type,
                label,
                label,
                description,
                domain,
                maxScore,
                maxScore,
                weight,
                eliminatory,
                "2026-07",
                List.of()
        );
    }
}
