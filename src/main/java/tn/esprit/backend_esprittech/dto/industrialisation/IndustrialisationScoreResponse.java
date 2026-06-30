package tn.esprit.backend_esprittech.dto.industrialisation;

import java.util.List;

public record IndustrialisationScoreResponse(
        Long candidatureId,
        Long projetId,
        Double scoreFinal,
        DecisionRecommandeeIndustrialisation decisionRecommandee,
        Boolean estBloqueParEliminatoire,
        List<BlocageEliminatoireDto> blocagesEliminatoires,
        Boolean hasEliminatoryWarnings,
        Integer eliminatoryWarningsCount,
        List<BlocageEliminatoireDto> eliminatoryWarningsDetails,
        Double scoreQuestions,
        Double scoreLivrables,
        Double scoreCriteresNotes,
        List<ScoreBreakdownDto> detailsCalcul,
        List<String> elementsManquants,
        String message
) {
    public IndustrialisationScoreResponse(
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
        this(candidatureId, projetId, scoreFinal, decisionRecommandee, estBloqueParEliminatoire, blocagesEliminatoires,
                estBloqueParEliminatoire, blocagesEliminatoires == null ? 0 : blocagesEliminatoires.size(),
                blocagesEliminatoires, scoreQuestions, scoreLivrables, scoreCriteresNotes, detailsCalcul, elementsManquants, message);
    }
}
