package tn.esprit.backend_esprittech.dto.critereeval;

import java.util.List;

public record CriterionConfigurationIssueResponse(
        Long criterionId,
        String criterionName,
        String criterionType,
        List<String> blockingMissingFields,
        List<String> nonBlockingMissingFields,
        String configurationVersion,
        String consequence
) {
}
