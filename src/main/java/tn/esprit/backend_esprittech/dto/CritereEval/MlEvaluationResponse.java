package tn.esprit.backend_esprittech.dto.CritereEval;

import java.util.List;

public record MlEvaluationResponse(
        String status,
        String modelVersion,
        String analysisEngineVersion,
        Double globalConfidence,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        List<String> warnings,
        List<MlCriterionResult> results
) {
}
