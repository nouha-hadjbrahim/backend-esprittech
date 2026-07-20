package tn.esprit.backend_esprittech.dto.industrialisation;

import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;

import java.time.LocalDateTime;

public record ReponseIndustrialisationResponse(
        Long id,
        Long questionId,
        String questionLibelle,
        String valeurTexte,
        Boolean valeurBoolean,
        Double valeurNumerique,
        String valeurUrl,
        String preuveObjectName,
        String preuveOriginalFileName,
        String preuveContentType,
        Long preuveSize,
        ReponseEliminatoire reponseEliminatoire,
        Double noteObtenue,
        String justificatif,
        LocalDateTime dateReponse
) {
    public ReponseIndustrialisationResponse(
            Long id,
            Long questionId,
            String valeurTexte,
            Boolean valeurBoolean,
            Double valeurNumerique,
            String valeurUrl,
            String preuveObjectName,
            String preuveOriginalFileName,
            String preuveContentType,
            Long preuveSize,
            ReponseEliminatoire reponseEliminatoire,
            Double noteObtenue,
            String justificatif,
            LocalDateTime dateReponse
    ) {
        this(id, questionId, null, valeurTexte, valeurBoolean, valeurNumerique, valeurUrl,
                preuveObjectName, preuveOriginalFileName, preuveContentType, preuveSize,
                reponseEliminatoire, noteObtenue, justificatif, dateReponse);
    }
}
