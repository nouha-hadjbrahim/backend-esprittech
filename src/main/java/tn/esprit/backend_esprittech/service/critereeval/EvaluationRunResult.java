package tn.esprit.backend_esprittech.service.critereeval;

import tn.esprit.backend_esprittech.dto.critereeval.EvaluationResponse;

public record EvaluationRunResult(
        Long projectId,
        EvaluationTrigger trigger,
        String correlationId,
        String inputSnapshotHash,
        String engineVersion,
        EvaluationResponse evaluation
) {
}
