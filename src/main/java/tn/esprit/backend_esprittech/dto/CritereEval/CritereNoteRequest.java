package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CritereNoteRequest(
        @NotBlank String libelle,
        String description,
        @NotBlank String domaine,
        @NotNull @Min(1) Integer ordre,
        @NotNull @Positive Double bareme,
        @NotNull @Positive Double poids,
        @NotNull Double seuil,
        Boolean actif
) {
}