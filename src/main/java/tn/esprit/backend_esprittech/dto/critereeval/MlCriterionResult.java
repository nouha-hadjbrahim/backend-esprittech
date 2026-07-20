package tn.esprit.backend_esprittech.dto.critereeval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlCriterionResult(
        Long criterionId,
        String type,
        String status,
        Double score,
        Double maxScore,
        Double normalizedScore,
        Double confidence,
        Double semanticConfidence,
        Double overallConfidence,
        Double evidenceQuality,
        Double extractionQuality,
        Double sourceCoverage,
        Boolean satisfied,
        String eliminatoryState,
        String justification,
        List<String> analysisMethods,
        List<MlEvidenceResponse> evidence,
        List<String> weaknessCodes,
        List<String> recommendationCodes,
        List<String> reasonCodes,
        String scoreBand,
        Double supportStrength,
        Double contradictionStrength,
        Double projectSpecificity,
        Double stanceConfidence,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations
) {
    public MlCriterionResult(
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
        this(
                criterionId,
                type,
                normalizedScore == null ? "INSUFFICIENT_EVIDENCE" : "SCORED",
                score,
                maxScore,
                normalizedScore,
                confidence,
                null,
                confidence,
                null,
                null,
                null,
                satisfied,
                statutDepuisSatisfied(satisfied),
                justification,
                List.of(),
                evidence,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                strengths,
                weaknesses,
                recommendations
        );
    }

    /** Statut lisible derive du drapeau {@code satisfied} ({@code null} si non evalue). */
    private static String statutDepuisSatisfied(Boolean satisfied) {
        if (satisfied == null) {
            return null;
        }
        return satisfied ? "OK" : "NOT_OK";
    }
}
