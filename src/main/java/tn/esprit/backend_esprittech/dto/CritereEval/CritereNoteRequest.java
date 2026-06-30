package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;

public record CritereNoteRequest(
        @NotBlank String libelle,
        String description,
        @NotBlank String domaine,
        @NotNull @Min(1) Integer ordre,
        @Positive Double bareme,
        @NotNull @Positive Double poids,
        Double seuil,
        @Min(1) @Max(5) Integer defaultNoteValue,
        Boolean actif,
        ModeEvaluation modeEvaluation,
        String expectedLivrableTypes,
        Integer minLivrableCount,
        String expectedKeyword,
        Double noteMaxAuto,
        Boolean ruleEnabled,
        String ruleDescription
) {
    public CritereNoteRequest(
            String libelle,
            String description,
            String domaine,
            Integer ordre,
            Double bareme,
            Double poids,
            Double seuil,
            Boolean actif
    ) {
        this(libelle, description, domaine, ordre, bareme, poids, seuil, 3, actif, null, null, null, null, null, false, null);
    }
}
