package tn.esprit.backend_esprittech.dto.critereeval;


import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.critereeval.ModeEvaluation;

import java.util.List;

public record ResultatCritereResponse(
        Long id,
        Long critereId,
        String critereLibelle,
        String critereDescription,
        String critereDomaine,
        TypeCritere typeCritere,
        ModeEvaluation modeEvaluation,
        ReponseEliminatoire reponseEliminatoire,
        Double noteObtenue,
        Double scorePondere,
        Double bareme,
        Double poids,
        Integer noteValue,
        String noteLabel,
        String matchedRule,
        String metadataUsed,
        String explanation,
        String evidenceSummary,
        List<Long> evidenceLivrableIds,
        Boolean ruleConfigured,
        String commentaire,
        Double mlScore,
        Double mlMaxScore,
        Double normalizedScore,
        Double confidence,
        String criterionStatus,
        Double evidenceQuality,
        Double extractionQuality,
        Double sourceCoverage,
        String eliminatoryState,
        List<String> analysisMethods,
        String evidenceJson,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations
) {
    public ResultatCritereResponse(
            Long id,
            Long critereId,
            String critereLibelle,
            String critereDescription,
            String critereDomaine,
            TypeCritere typeCritere,
            ModeEvaluation modeEvaluation,
            ReponseEliminatoire reponseEliminatoire,
            Double noteObtenue,
            Double scorePondere,
            Double bareme,
            Double poids,
            Integer noteValue,
            String noteLabel,
            String matchedRule,
            String metadataUsed,
            String explanation,
            String evidenceSummary,
            List<Long> evidenceLivrableIds,
            Boolean ruleConfigured,
            String commentaire
    ) {
        this(id, critereId, critereLibelle, critereDescription, critereDomaine,
                typeCritere, modeEvaluation, reponseEliminatoire, noteObtenue,
                scorePondere, bareme, poids, noteValue, noteLabel, matchedRule,
                metadataUsed, explanation, evidenceSummary, evidenceLivrableIds,
                ruleConfigured, commentaire, null, null, null, null,
                null, null, null, null, null, List.of(), null,
                List.of(), List.of(), List.of());
    }
}
