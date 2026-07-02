package tn.esprit.backend_esprittech.dto.CritereEval;

public record MlDeliverableContent(
        Long deliverableId,
        String deliverableType,
        String name,
        String contentType,
        String extractionStatus,
        String unsupportedReason,
        String content
) {
}
