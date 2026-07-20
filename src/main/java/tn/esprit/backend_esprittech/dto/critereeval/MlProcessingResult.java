package tn.esprit.backend_esprittech.dto.critereeval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlProcessingResult(
        Long deliverableId,
        String filename,
        String sourceType,
        String status,
        String language,
        String contentHash,
        Integer pageCount,
        String extractionMethod,
        List<String> warnings,
        Map<String, Object> metrics,
        String reference
) {
}
