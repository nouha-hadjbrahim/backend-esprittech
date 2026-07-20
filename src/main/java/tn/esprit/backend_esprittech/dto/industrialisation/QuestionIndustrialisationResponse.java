package tn.esprit.backend_esprittech.dto.industrialisation;

import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;

import java.time.LocalDateTime;

public record QuestionIndustrialisationResponse(
        Long id,
        String libelle,
        String description,
        TypeReponseIndustrialisation typeReponse,
        Boolean obligatoire,
        TypeCritere typeCritere,
        Double poids,
        Integer ordre,
        Boolean actif,
        Boolean conditionEliminatoire,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour
) {
}
