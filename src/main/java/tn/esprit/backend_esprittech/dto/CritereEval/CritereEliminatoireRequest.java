package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;

public record CritereEliminatoireRequest(
        @NotBlank String libelle,
        String description,
        @NotBlank String domaine,
        @NotNull @Min(1) Integer ordre,
        @NotNull ReponseEliminatoire reponseAttendue,
        Boolean actif,
        ModeEvaluation modeEvaluation,
        String expectedLivrableTypes,
        Integer minLivrableCount,
        String expectedKeyword,
        Double noteMaxAuto,
        Boolean ruleEnabled,
        String ruleDescription
) {
    public CritereEliminatoireRequest(
            String libelle,
            String description,
            String domaine,
            Integer ordre,
            ReponseEliminatoire reponseAttendue,
            Boolean actif
    ) {
        this(libelle, description, domaine, ordre, reponseAttendue, actif, null, null, null, null, null, false, null);
    }
}
