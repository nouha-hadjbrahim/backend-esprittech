package tn.esprit.backend_esprittech.dto.critereeval;


import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;

public record ResultatEliminatoireRequest(
        @NotNull Long critereId,
        @NotNull ReponseEliminatoire reponse,
        String commentaire
) {
}