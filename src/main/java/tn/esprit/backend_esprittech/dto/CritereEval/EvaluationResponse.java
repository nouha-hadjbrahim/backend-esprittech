package tn.esprit.backend_esprittech.dto.CritereEval;


import java.time.LocalDateTime;
import java.util.List;

public record EvaluationResponse(
        Long id,
        Long sujetProjetId,
        Double scoreFinal,
        Boolean eligibleIndustrialisation,
        Boolean bloqueParEliminatoire,
        LocalDateTime dateCalcul,
        String commentaire,
        List<ResultatCritereResponse> resultats
) {
}