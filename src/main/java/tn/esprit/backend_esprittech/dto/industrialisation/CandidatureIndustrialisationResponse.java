package tn.esprit.backend_esprittech.dto.industrialisation;

import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.entity.industrialisation.OrientationIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.time.LocalDateTime;
import java.util.List;

public record CandidatureIndustrialisationResponse(
        Long id,
        Long projetId,
        String projetTitre,
        StatutSujet projetStatut,
        CategorieSujet projetCategorie,
        String projetDomaine,
        String projetDescription,
        String projetObjectifs,
        String projetTechnologies,
        String projetPrerequis,
        LocalDateTime projetDateCreation,
        LocalDateTime projetDateSoumission,
        LocalDateTime projetDateValidation,
        LocalDateTime projetDateTerminaison,
        Double scoreEvaluationProjet,
        Boolean eligibleIndustrialisation,
        Boolean bloqueParEliminatoire,
        Boolean hasEliminatoryWarnings,
        Integer eliminatoryWarningsCount,
        List<String> eliminatoryWarningsDetails,
        TypeIndustrialisation typeIndustrialisation,
        StatutIndustrialisation statut,
        Long demandeurId,
        String demandeurNom,
        LocalDateTime dateDemande,
        LocalDateTime dateSoumission,
        LocalDateTime dateReceptionCI,
        Long ciDecideurId,
        String ciDecideurNom,
        LocalDateTime dateDecisionCI,
        Boolean decisionGoNoGo,
        OrientationIndustrialisation orientation,
        String motifDecision,
        String commentaire,
        List<ReponseIndustrialisationResponse> reponses,
        List<LivrableResponse> livrables,
        List<HistoriqueIndustrialisationResponse> historique,
        EvaluationResponse latestEvaluation,
        List<String> warnings
) {
    public CandidatureIndustrialisationResponse(
            Long id,
            Long projetId,
            String projetTitre,
            StatutSujet projetStatut,
            Double scoreEvaluationProjet,
            Boolean eligibleIndustrialisation,
            Boolean bloqueParEliminatoire,
            TypeIndustrialisation typeIndustrialisation,
            StatutIndustrialisation statut,
            Long demandeurId,
            String demandeurNom,
            LocalDateTime dateDemande,
            LocalDateTime dateSoumission,
            LocalDateTime dateReceptionCI,
            Long ciDecideurId,
            String ciDecideurNom,
            LocalDateTime dateDecisionCI,
            Boolean decisionGoNoGo,
            OrientationIndustrialisation orientation,
            String motifDecision,
            String commentaire,
            List<ReponseIndustrialisationResponse> reponses,
            List<LivrableResponse> livrables,
            List<HistoriqueIndustrialisationResponse> historique
    ) {
        this(id, projetId, projetTitre, projetStatut, null, null, null, null, null, null, null, null, null, null,
                scoreEvaluationProjet, eligibleIndustrialisation, bloqueParEliminatoire, Boolean.TRUE.equals(bloqueParEliminatoire),
                Boolean.TRUE.equals(bloqueParEliminatoire) ? 1 : 0, List.of(), typeIndustrialisation, statut, demandeurId, demandeurNom,
                dateDemande, dateSoumission, dateReceptionCI, ciDecideurId, ciDecideurNom,
                dateDecisionCI, decisionGoNoGo, orientation, motifDecision, commentaire,
                reponses, livrables, historique, null, List.of());
    }
}
