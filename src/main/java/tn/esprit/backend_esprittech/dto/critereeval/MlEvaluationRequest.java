package tn.esprit.backend_esprittech.dto.critereeval;

import java.util.List;

public record MlEvaluationRequest(
        Long projectId,
        String correlationId,
        String criteriaVersion,
        String engineVersion,
        String inputSnapshotHash,
        String projectSnapshotHash,
        String deliverableSnapshotHash,
        MlProjectContext project,
        List<MlCriterionRequest> criteria,
        List<MlDeliverableContent> deliverables,
        List<MlExternalSourceRequest> externalSources
) {
    public MlEvaluationRequest(
            MlProjectContext project,
            List<MlCriterionRequest> criteria,
            List<MlDeliverableContent> deliverables
    ) {
        this(
                project == null ? null : project.projectId(),
                "",
                "unversioned",
                "multimodal-semantic-v2",
                null,
                null,
                null,
                project,
                criteria,
                deliverables,
                List.of()
        );
    }
}
