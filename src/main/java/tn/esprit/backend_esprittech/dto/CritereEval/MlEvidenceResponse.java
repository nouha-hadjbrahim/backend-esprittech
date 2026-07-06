package tn.esprit.backend_esprittech.dto.CritereEval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlEvidenceResponse(
        Long deliverableId,
        String sourceType,
        String source,
        Integer page,
        String section,
        String path,
        String commitHash,
        String contentHash,
        String extractionMethod,
        String excerpt,
        Double relevance,
        List<String> matchedKeywords
) {
    public MlEvidenceResponse(
            Long deliverableId,
            String excerpt,
            Double relevance,
            List<String> matchedKeywords
    ) {
        this(
                deliverableId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                excerpt,
                relevance,
                matchedKeywords
        );
    }
}
