package tn.esprit.backend_esprittech.dto.industrialisation;

import java.util.List;

public record IndustrialisationFormResponse(
        CandidatureIndustrialisationResponse candidature,
        List<QuestionIndustrialisationResponse> questions,
        List<ReponseIndustrialisationResponse> reponses
) {
}
