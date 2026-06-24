package tn.esprit.backend_esprittech.dto.CritereEval;


import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;

public record ResultatCritereResponse(
        Long id,
        Long critereId,
        TypeCritere typeCritere,
        ReponseEliminatoire reponseEliminatoire,
        Double noteObtenue,
        Double scorePondere,
        String commentaire
) {
}