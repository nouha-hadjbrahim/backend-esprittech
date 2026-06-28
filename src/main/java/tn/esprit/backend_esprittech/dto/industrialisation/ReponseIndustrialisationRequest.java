package tn.esprit.backend_esprittech.dto.industrialisation;

import jakarta.validation.constraints.NotNull;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;

public record ReponseIndustrialisationRequest(
        @NotNull Long questionId,
        String valeurTexte,
        Boolean valeurBoolean,
        Double valeurNumerique,
        String valeurUrl,
        ReponseEliminatoire reponseEliminatoire,
        Double noteObtenue,
        String justificatif
) {
}
