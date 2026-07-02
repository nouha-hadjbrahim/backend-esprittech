package tn.esprit.backend_esprittech.dto.CritereEval;

import java.util.List;

public record MlEvidenceResponse(
        Long deliverableId,
        String excerpt,
        Double relevance,
        List<String> matchedKeywords
) {
}
