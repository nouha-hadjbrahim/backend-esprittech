package tn.esprit.backend_esprittech.dto.CritereEval;


import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;

import java.time.LocalDateTime;

public record CritereEliminatoireResponse(
        Long id,
        String libelle,
        String description,
        String domaine,
        Integer ordre,
        Boolean actif,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour,
        ReponseEliminatoire reponseAttendue,
        ModeEvaluation modeEvaluation,
        String expectedLivrableTypes,
        Integer minLivrableCount,
        String expectedKeyword,
        Double noteMaxAuto,
        Boolean ruleEnabled,
        String ruleDescription
) {
    public CritereEliminatoireResponse(
            Long id,
            String libelle,
            String description,
            String domaine,
            Integer ordre,
            Boolean actif,
            LocalDateTime dateCreation,
            LocalDateTime dateMiseAJour,
            ReponseEliminatoire reponseAttendue
    ) {
        this(id, libelle, description, domaine, ordre, actif, dateCreation, dateMiseAJour, reponseAttendue,
                null, null, null, null, null, false, null);
    }
}
