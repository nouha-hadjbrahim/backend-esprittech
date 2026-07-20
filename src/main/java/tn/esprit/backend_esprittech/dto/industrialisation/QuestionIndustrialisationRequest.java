package tn.esprit.backend_esprittech.dto.industrialisation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;

public record QuestionIndustrialisationRequest(
        @NotBlank @Size(max = 255) String libelle,
        @Size(max = 2000) String description,
        @NotNull TypeReponseIndustrialisation typeReponse,
        Boolean obligatoire,
        @NotNull TypeCritere typeCritere,
        Double poids,
        @NotNull @Min(1) Integer ordre,
        Boolean actif,
        Boolean conditionEliminatoire
) {
}
