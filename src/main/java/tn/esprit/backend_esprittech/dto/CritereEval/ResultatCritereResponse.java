package tn.esprit.backend_esprittech.dto.CritereEval;


import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;

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
        String commentaire
) {
}
