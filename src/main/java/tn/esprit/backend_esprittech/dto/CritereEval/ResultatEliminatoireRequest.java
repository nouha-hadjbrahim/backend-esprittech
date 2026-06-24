package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;

public record ResultatEliminatoireRequest(
        @NotNull Long critereId,
        @NotNull ReponseEliminatoire reponse,
        String commentaire
) {
}