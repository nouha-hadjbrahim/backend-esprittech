package tn.esprit.backend_esprittech.dto.CritereEval;


import java.time.LocalDateTime;
import java.util.List;

public record EvaluationResponse(
        Long id,
        Long sujetProjetId,
        Double scoreFinal,
        Boolean eligibleIndustrialisation,
        Boolean bloqueParEliminatoire,
        Boolean hasEliminatoryWarnings,
        Integer eliminatoryWarningsCount,
        List<String> eliminatoryWarningsDetails,
        LocalDateTime dateCalcul,
        String commentaire,
        String calculatedBy,
        String recalculationReason,
        List<ResultatCritereResponse> resultats,
        Integer totalCriteresNotes,
        Integer criteresNotesConfigures,
        Integer criteresNotesNonConfigures,
        Integer totalCriteresEliminatoires,
        Integer criteresEliminatoiresSatisfaits,
        Integer criteresEliminatoiresNonConfigures,
        Boolean evaluationComplete,
        String scoreCalculationCoverageMessage,
        List<String> blockingCriteriaNames,
        String mlStatus,
        String mlModelVersion,
        Double mlGlobalConfidence,
        Double mlScore,
        Double finalValidatedScore,
        String validationStatus,
        String validatedBy,
        LocalDateTime validatedAt,
        String overrideReason,
        List<String> mlWarnings,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        String errorMessage
) {
    public EvaluationResponse(
            Long id,
            Long sujetProjetId,
            Double scoreFinal,
            Boolean eligibleIndustrialisation,
            Boolean bloqueParEliminatoire,
            Boolean hasEliminatoryWarnings,
            Integer eliminatoryWarningsCount,
            List<String> eliminatoryWarningsDetails,
            LocalDateTime dateCalcul,
            String commentaire,
            String calculatedBy,
            String recalculationReason,
            List<ResultatCritereResponse> resultats,
            Integer totalCriteresNotes,
            Integer criteresNotesConfigures,
            Integer criteresNotesNonConfigures,
            Integer totalCriteresEliminatoires,
            Integer criteresEliminatoiresSatisfaits,
            Integer criteresEliminatoiresNonConfigures,
            Boolean evaluationComplete,
            String scoreCalculationCoverageMessage,
            List<String> blockingCriteriaNames
    ) {
        this(id, sujetProjetId, scoreFinal, eligibleIndustrialisation,
                bloqueParEliminatoire, hasEliminatoryWarnings,
                eliminatoryWarningsCount, eliminatoryWarningsDetails, dateCalcul,
                commentaire, calculatedBy, recalculationReason, resultats,
                totalCriteresNotes, criteresNotesConfigures, criteresNotesNonConfigures,
                totalCriteresEliminatoires, criteresEliminatoiresSatisfaits,
                criteresEliminatoiresNonConfigures, evaluationComplete,
                scoreCalculationCoverageMessage, blockingCriteriaNames,
                null, null, null, scoreFinal, null, "PENDING", null, null, null,
                List.of(), List.of(), List.of(), List.of(), null);
    }

    public EvaluationResponse(
            Long id,
            Long sujetProjetId,
            Double scoreFinal,
            Boolean eligibleIndustrialisation,
            Boolean bloqueParEliminatoire,
            LocalDateTime dateCalcul,
            String commentaire,
            String calculatedBy,
            String recalculationReason,
            List<ResultatCritereResponse> resultats
    ) {
        this(id, sujetProjetId, scoreFinal, eligibleIndustrialisation, bloqueParEliminatoire,
                Boolean.TRUE.equals(bloqueParEliminatoire), 0, List.of(), dateCalcul,
                commentaire, calculatedBy, recalculationReason, resultats,
                null, null, null, null, null, null, null, null, List.of(),
                null, null, null, scoreFinal, null, "PENDING", null, null, null,
                List.of(), List.of(), List.of(), List.of(), null);
    }
}
