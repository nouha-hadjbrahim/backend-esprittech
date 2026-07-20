package tn.esprit.backend_esprittech.service.critereeval;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.entity.critereeval.CritereEvaluation;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNote;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;

import java.util.ArrayList;
import java.util.List;

@Component
public class CriterionConfigurationPolicy {

    public ConfigurationReport inspect(CritereEvaluation critere, TypeCritere type) {
        List<String> blocking = new ArrayList<>();
        List<String> nonBlocking = new ArrayList<>();

        require(blocking, "id", critere.getId() != null);
        require(blocking, "name", StringUtils.hasText(critere.getLibelle()));
        require(blocking, "type", type != null);
        require(blocking, "criterionFamily", StringUtils.hasText(critere.getCriterionFamily()));
        require(blocking, "version", StringUtils.hasText(critere.getConfigurationVersion()));
        require(blocking, "applicability", StringUtils.hasText(critere.getApplicability()));
        require(blocking, "activeState", critere.getActif() != null);
        require(blocking, "description", StringUtils.hasText(critere.getDescription()));

        if (type == TypeCritere.NOTE) {
            CritereNote note = (CritereNote) critere;
            require(blocking, "weight", note.getPoids() != null && note.getPoids() > 0.0);
            require(blocking, "maximumScore", note.getBareme() != null && note.getBareme() > 0.0);
            require(blocking, "positiveEvidenceExpectations", StringUtils.hasText(critere.getPositiveEvidenceExpectations()));
            require(blocking, "requiredEvidenceTypes", StringUtils.hasText(critere.getRequiredEvidenceTypes()));
            require(blocking, "minimumSupportingEvidenceCount", critere.getMinimumSupportingEvidenceCount() != null
                    && critere.getMinimumSupportingEvidenceCount() > 0);
            require(blocking, "scoringPolicy", StringUtils.hasText(critere.getScoringPolicy()));
            optional(nonBlocking, "negativeEvidenceExpectations", critere.getNegativeEvidenceExpectations());
            optional(nonBlocking, "permittedEvidenceNatures", critere.getPermittedEvidenceNatures());
        } else if (type == TypeCritere.ELIMINATOIRE) {
            require(blocking, "positiveFactualCondition", StringUtils.hasText(critere.getPositiveEvidenceExpectations()));
            require(blocking, "confirmedNegativeFactualCondition", StringUtils.hasText(critere.getNegativeEvidenceExpectations()));
            require(blocking, "permittedAuthoritativeSourceTypes", StringUtils.hasText(critere.getRequiredEvidenceTypes()));
            require(blocking, "permittedEvidenceNatures", StringUtils.hasText(critere.getPermittedEvidenceNatures()));
            require(blocking, "decisionRule", StringUtils.hasText(critere.getEliminatoryDecisionRule()));
            require(blocking, "indeterminateBehaviour", StringUtils.hasText(critere.getEliminatoryDecisionRule())
                    && critere.getEliminatoryDecisionRule().toUpperCase().contains("INDETERMINATE"));
        }

        optional(nonBlocking, "optionalEvidenceTypes", critere.getOptionalEvidenceTypes());
        optional(nonBlocking, "sourceAuthorityRules", critere.getSourceAuthorityRules());

        return new ConfigurationReport(
                blocking.isEmpty(),
                List.copyOf(blocking),
                List.copyOf(nonBlocking),
                critere.getConfigurationVersion(),
                blocking.isEmpty()
                        ? List.of("CRITERION_CONFIGURATION_COMPLETE")
                        : List.of("CRITERION_CONFIGURATION_INCOMPLETE")
        );
    }

    public void requireActiveComplete(CritereEvaluation critere, TypeCritere type) {
        if (!Boolean.TRUE.equals(critere.getActif())) {
            return;
        }
        ConfigurationReport report = inspect(critere, type);
        List<String> blocking = report.blockingMissingFields().stream()
                .filter(field -> !("id".equals(field) && critere.getId() == null))
                .toList();
        if (!blocking.isEmpty()) {
            throw new tn.esprit.backend_esprittech.exception.critereeval.BadRequestException(
                    "Configuration du critere incomplete: " + String.join(", ", blocking)
            );
        }
    }

    private void require(List<String> missing, String field, boolean condition) {
        if (!condition) {
            missing.add(field);
        }
    }

    private void optional(List<String> missing, String field, String value) {
        if (!StringUtils.hasText(value)) {
            missing.add(field);
        }
    }

    public record ConfigurationReport(
            boolean configurationComplete,
            List<String> blockingMissingFields,
            List<String> nonBlockingMissingFields,
            String configurationVersion,
            List<String> configurationReasonCodes
    ) {
    }
}
