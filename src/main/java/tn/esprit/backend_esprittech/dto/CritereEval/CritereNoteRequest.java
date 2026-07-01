package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;

public record CritereNoteRequest(
        @NotBlank(message = "Le libelle est obligatoire.") String libelle,
        String description,
        @NotBlank(message = "Le domaine est obligatoire.") String domaine,
        @NotNull(message = "L'ordre est obligatoire.") @Min(value = 1, message = "L'ordre doit etre superieur ou egal a 1.") Integer ordre,
        @Positive(message = "Le bareme doit etre superieur a 0.") Double bareme,
        @NotNull(message = "Le poids est obligatoire.") @Positive(message = "Le poids doit etre superieur a 0.") Double poids,
        Double seuil,
        @Min(value = 1, message = "La note par defaut doit etre superieure ou egale a 1.")
        Integer defaultNoteValue,
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
