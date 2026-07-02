package tn.esprit.backend_esprittech.dto.CritereEval;

import java.util.List;

public record MlEvaluationRequest(
        MlProjectContext project,
        List<MlCriterionRequest> criteria,
        List<MlDeliverableContent> deliverables
) {
}
