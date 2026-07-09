package tn.esprit.backend_esprittech.service.CritereEval;

import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;

public record EvaluationRunResult(
        Long projectId,
        EvaluationTrigger trigger,
        String correlationId,
        String inputSnapshotHash,
        String engineVersion,
        EvaluationResponse evaluation
) {
}
