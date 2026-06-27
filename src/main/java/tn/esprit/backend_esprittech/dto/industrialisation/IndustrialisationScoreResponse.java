package tn.esprit.backend_esprittech.dto.industrialisation;

import java.util.List;

public record IndustrialisationScoreResponse(
        Long candidatureId,
        Long projetId,
        Double scoreFinal,
        DecisionRecommandeeIndustrialisation decisionRecommandee,
        Boolean estBloqueParEliminatoire,
        List<BlocageEliminatoireDto> blocagesEliminatoires,
        Double scoreQuestions,
        Double scoreLivrables,
        Double scoreCriteresNotes,
        List<ScoreBreakdownDto> detailsCalcul,
        List<String> elementsManquants,
        String message
) {
}
