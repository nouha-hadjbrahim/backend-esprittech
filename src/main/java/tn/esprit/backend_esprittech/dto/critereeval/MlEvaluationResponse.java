package tn.esprit.backend_esprittech.dto.critereeval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlEvaluationResponse(
        String evaluationStatus,
        String status,
        String engineType,
        String modelName,
        String modelVersion,
        String modelRevision,
        String pipelineVersion,
        String configurationVersion,
        String analysisEngineVersion,
        String criteriaVersion,
        String inputSnapshotHash,
        String correlationId,
        List<String> languages,
        Map<String, Object> embedding,
        Map<String, Object> timing,
        Double globalConfidence,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        List<String> warnings,
        List<MlCriterionResult> criteria,
        List<MlProcessingResult> deliverableProcessing,
        List<MlProcessingResult> externalSourceProcessing,
        List<MlCriterionResult> results
) {
    public MlEvaluationResponse(
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
        this(
                status,
                status,
                null,
                null,
                modelVersion,
                null,
                analysisEngineVersion,
                null,
                analysisEngineVersion,
                null,
                null,
                null,
                List.of(),
                Map.of(),
                Map.of(),
                globalConfidence,
                strengths,
                weaknesses,
                recommendations,
                warnings,
                results,
                List.of(),
                List.of(),
                results
        );
    }

    public String effectiveStatus() {
        return evaluationStatus != null ? evaluationStatus : status;
    }

    public String effectivePipelineVersion() {
        return pipelineVersion != null ? pipelineVersion : analysisEngineVersion;
    }

    public List<MlCriterionResult> effectiveResults() {
        return results != null ? results : criteria;
    }
}
