package tn.esprit.backend_esprittech.dto.CritereEval;

import java.util.List;

public record MlCriterionResult(
        Long criterionId,
        String type,
        Double score,
        Double maxScore,
        Double normalizedScore,
        Double confidence,
        Boolean satisfied,
        String justification,
        List<MlEvidenceResponse> evidence,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations
) {
}
