package tn.esprit.backend_esprittech.dto.CritereEval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlEvidenceResponse(
        String evidenceId,
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
        Double rawSemanticSimilarity,
        Double calibratedRelevance,
        Double projectRelevanceScore,
        Double rankingMargin,
        String stance,
        String projectRelevanceType,
        Double stanceConfidence,
        Double sourceAuthority,
        Double contradictionStrength,
        Double evidenceQuality,
        Double extractionQuality,
        String documentCategory,
        Double documentCategoryConfidence,
        List<String> documentCategoryReasonCodes,
        String evidenceNature,
        List<String> matchedKeywords
) {

    /**
     * Ensures that matchedKeywords is never null.
     */
    public MlEvidenceResponse {
        documentCategoryReasonCodes = documentCategoryReasonCodes == null
                ? List.of()
                : List.copyOf(documentCategoryReasonCodes);
        matchedKeywords = matchedKeywords == null
                ? List.of()
                : List.copyOf(matchedKeywords);
    }

    /**
     * Simplified constructor used by legacy code and unit tests.
     */
    public MlEvidenceResponse(
            Long deliverableId,
            String excerpt,
            Double relevance,
            List<String> matchedKeywords
    ) {
        this(
                null,                       // evidenceId
                deliverableId,              // deliverableId
                null,                       // sourceType
                null,                       // source
                null,                       // page
                null,                       // section
                null,                       // path
                null,                       // commitHash
                null,                       // contentHash
                null,                       // extractionMethod
                excerpt,                    // excerpt
                relevance,                  // relevance
                null,                       // rawSemanticSimilarity
                null,                       // calibratedRelevance
                null,                       // projectRelevanceScore
                null,                       // rankingMargin
                null,                       // stance
                null,                       // projectRelevanceType
                null,                       // stanceConfidence
                null,                       // sourceAuthority
                null,                       // contradictionStrength
                null,                       // evidenceQuality
                null,                       // extractionQuality
                null,                       // documentCategory
                null,                       // documentCategoryConfidence
                List.<String>of(),          // documentCategoryReasonCodes
                null,                       // evidenceNature
                matchedKeywords             // matchedKeywords
        );
    }

    /**
     * Simplified constructor used to create evidence with a stance and
     * an optional contradiction strength.
     */
    public MlEvidenceResponse(
            Long deliverableId,
            String excerpt,
            Double relevance,
            String stance,
            Double contradictionStrength
    ) {
        this(
                null,                       // evidenceId
                deliverableId,              // deliverableId
                null,                       // sourceType
                null,                       // source
                null,                       // page
                null,                       // section
                null,                       // path
                null,                       // commitHash
                null,                       // contentHash
                null,                       // extractionMethod
                excerpt,                    // excerpt
                relevance,                  // relevance
                null,                       // rawSemanticSimilarity
                relevance,                  // calibratedRelevance
                null,                       // projectRelevanceScore
                null,                       // rankingMargin
                stance,                     // stance
                "PROJECT_SPECIFIC",         // projectRelevanceType
                0.8,                        // stanceConfidence
                0.8,                        // sourceAuthority
                contradictionStrength,      // contradictionStrength
                0.8,                        // evidenceQuality
                0.9,                        // extractionQuality
                "PROJECT_DELIVERABLE",      // documentCategory
                0.9,                        // documentCategoryConfidence
                List.<String>of(),          // documentCategoryReasonCodes
                "SEMANTIC",                 // evidenceNature
                List.<String>of()            // matchedKeywords
        );
    }
}
