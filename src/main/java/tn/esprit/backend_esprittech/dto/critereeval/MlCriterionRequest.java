package tn.esprit.backend_esprittech.dto.critereeval;

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
        List<String> requiredEvidenceTypes,
        String criterionFamily,
        String applicability,
        String configurationVersion,
        List<String> positiveEvidenceExpectations,
        List<String> negativeEvidenceExpectations,
        List<String> permittedEvidenceNatures,
        Integer minimumSupportingEvidenceCount,
        String scoringPolicy,
        String eliminatoryDecisionRule,
        Boolean strictConfiguration
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
                List.of(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                false
        );
    }
}
