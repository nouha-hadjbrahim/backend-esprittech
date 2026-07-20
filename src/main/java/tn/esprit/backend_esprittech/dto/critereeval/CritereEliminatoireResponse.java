package tn.esprit.backend_esprittech.dto.critereeval;


import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.ModeEvaluation;

import java.time.LocalDateTime;
import java.util.List;

public record CritereEliminatoireResponse(
        Long id,
        String libelle,
        String description,
        String domaine,
        Integer ordre,
        Boolean actif,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour,
        ReponseEliminatoire reponseAttendue,
        ModeEvaluation modeEvaluation,
        String expectedLivrableTypes,
        Integer minLivrableCount,
        String expectedKeyword,
        Double noteMaxAuto,
        Boolean ruleEnabled,
        String ruleDescription,
        String criterionFamily,
        String configurationVersion,
        String applicability,
        String requiredEvidenceTypes,
        String optionalEvidenceTypes,
        String positiveEvidenceExpectations,
        String negativeEvidenceExpectations,
        String permittedEvidenceNatures,
        Integer minimumSupportingEvidenceCount,
        String scoringPolicy,
        String eliminatoryDecisionRule,
        String sourceAuthorityRules,
        Boolean configurationComplete,
        List<String> blockingMissingFields,
        List<String> nonBlockingMissingFields,
        List<String> configurationReasonCodes
) {
    public CritereEliminatoireResponse(
            Long id,
            String libelle,
            String description,
            String domaine,
            Integer ordre,
            Boolean actif,
            LocalDateTime dateCreation,
            LocalDateTime dateMiseAJour,
            ReponseEliminatoire reponseAttendue
    ) {
        this(id, libelle, description, domaine, ordre, actif, dateCreation, dateMiseAJour, reponseAttendue,
                null, null, null, null, null, false, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                true, List.of(), List.of(), List.of());
    }
}
