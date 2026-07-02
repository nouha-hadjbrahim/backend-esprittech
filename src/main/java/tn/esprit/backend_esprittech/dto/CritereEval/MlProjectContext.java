package tn.esprit.backend_esprittech.dto.CritereEval;

public record MlProjectContext(
        Long projectId,
        String title,
        String domain,
        String technologies,
        String category
) {
}
