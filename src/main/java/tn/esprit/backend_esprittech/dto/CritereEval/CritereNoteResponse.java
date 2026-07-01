package tn.esprit.backend_esprittech.dto.CritereEval;


import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;

import java.time.LocalDateTime;

public record CritereNoteResponse(
        Long id,
        String libelle,
        String description,
        String domaine,
        Integer ordre,
        Boolean actif,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour,
        Double bareme,
        Double poids,
        Double seuil,
        Integer defaultNoteValue,
        ModeEvaluation modeEvaluation,
        String expectedLivrableTypes,
        Integer minLivrableCount,
        String expectedKeyword,
        Double noteMaxAuto,
        Boolean ruleEnabled,
        String ruleDescription
) {
    public CritereNoteResponse(
            Long id,
            String libelle,
            String description,
            String domaine,
            Integer ordre,
            Boolean actif,
            LocalDateTime dateCreation,
            LocalDateTime dateMiseAJour,
            Double bareme,
            Double poids,
            Double seuil
    ) {
        this(id, libelle, description, domaine, ordre, actif, dateCreation, dateMiseAJour, bareme, poids, seuil,
                3, null, null, null, null, null, false, null);
    }
}
