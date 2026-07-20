package tn.esprit.backend_esprittech.dto.critereeval;

public record MlDeliverableContent(
        Long deliverableId,
        String deliverableType,
        String sourceType,
        String filename,
        String name,
        String mimeType,
        String contentType,
        Long fileSize,
        String objectName,
        String contentHash,
        String extractionStatus,
        String unsupportedReason,
        String content,
        String contentBase64
) {
    public MlDeliverableContent(
            Long deliverableId,
            String deliverableType,
            String name,
            String contentType,
            String extractionStatus,
            String unsupportedReason,
            String content
    ) {
        this(
                deliverableId,
                deliverableType,
                deliverableType,
                name,
                name,
                contentType,
                contentType,
                null,
                null,
                null,
                extractionStatus,
                unsupportedReason,
                content,
                null
        );
    }
}
