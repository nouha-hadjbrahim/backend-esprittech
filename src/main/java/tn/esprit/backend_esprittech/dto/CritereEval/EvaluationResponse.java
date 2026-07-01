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
        List<String> blockingCriteriaNames
) {
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
                null, null, null, null, null, null, null, null, List.of());
    }
}
