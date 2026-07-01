package tn.esprit.backend_esprittech.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNoteRule;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.NoteLevel;
import tn.esprit.backend_esprittech.entity.CritereEval.RuleOperator;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.NoteLevelRepository;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class EvaluationRuleInitializer implements CommandLineRunner {

    private final CritereEliminatoireRepository critereEliminatoireRepository;
    private final CritereNoteRepository critereNoteRepository;
    private final CritereNoteRuleRepository critereNoteRuleRepository;
    private final NoteLevelRepository noteLevelRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedNoteLevels();
        critereEliminatoireRepository.findAll().forEach(critere -> {
            if (critere.getModeEvaluation() == null && applyDefaultRule(critere)) {
                critereEliminatoireRepository.save(critere);
            }
        });
        critereNoteRepository.findAll().forEach(critere -> {
            boolean changed = false;
            if (critere.getDefaultNoteValue() == null) {
                critere.setDefaultNoteValue(3);
                changed = true;
            }
            if (critere.getBareme() == null || critere.getBareme() <= 0) {
                critere.setBareme(5.0);
                changed = true;
            }
            if (critere.getSeuil() == null) {
                critere.setSeuil(3.0);
                changed = true;
            }
            if (critere.getModeEvaluation() == null && applyDefaultRule(critere)) {
                changed = true;
            }
            if (Boolean.TRUE.equals(critere.getRuleEnabled())
                    && critere.getModeEvaluation() != null
                    && !critereNoteRuleRepository.existsByCritereNoteId(critere.getId())) {
                createRuleFromLegacyMode(critere);
            }
            if (changed) {
                critereNoteRepository.save(critere);
            }
        });
    }

    private void seedNoteLevels() {
        if (noteLevelRepository.count() > 0) {
            return;
        }
        List<String> labels = List.of(
                "Très insatisfait",
                "Insatisfait",
                "Peu satisfait",
                "Satisfait",
                "Très satisfait"
        );
        for (int i = 0; i < labels.size(); i++) {
            NoteLevel level = new NoteLevel();
            level.setValue(i + 1);
            level.setLabel(labels.get(i));
            level.setDescription(labels.get(i));
            level.setOrderIndex(i + 1);
            level.setActive(true);
            noteLevelRepository.save(level);
        }
    }

    private boolean applyDefaultRule(CritereEvaluation critere) {
        String label = normalize(critere.getLibelle());
        if (label.contains("documentation") || label.contains("document")) {
            setRule(critere, ModeEvaluation.DOCUMENTATION_PRESENT, "DOCUMENTATION,RAPPORT", 1,
                    "Documentation ou rapport requis pour satisfaire ce critere.");
            return true;
        }
        if (label.contains("git") || label.contains("code source")) {
            setRule(critere, ModeEvaluation.GIT_LINK_PRESENT, "LIEN_GIT,CODE_SOURCE", 1,
                    "Lien Git ou code source requis pour satisfaire ce critere.");
            return true;
        }
        if (label.contains("rapport")) {
            setRule(critere, ModeEvaluation.LIVRABLE_TYPE_PRESENT, "RAPPORT", 1,
                    "Rapport requis pour satisfaire ce critere.");
            return true;
        }
        if (label.contains("presentation")) {
            setRule(critere, ModeEvaluation.LIVRABLE_TYPE_PRESENT, "PRESENTATION", 1,
                    "Presentation requise pour satisfaire ce critere.");
            return true;
        }
        if (label.contains("richesse") || label.contains("livrable")) {
            setRule(critere, ModeEvaluation.LIVRABLE_COUNT_MIN, null, 3,
                    "Au moins trois livrables actifs sont requis.");
            return true;
        }
        if (critere instanceof CritereNote && (label.contains("qualite") || label.contains("technique"))) {
            setRule(critere, ModeEvaluation.GIT_LINK_PRESENT, "LIEN_GIT,CODE_SOURCE", 1,
                    "Code source ou lien Git utilise comme preuve pour la note.");
            return true;
        }
        return false;
    }

    private void createRuleFromLegacyMode(CritereNote critere) {
        CritereNoteRule rule = new CritereNoteRule();
        rule.setCritereNote(critere);
        rule.setRuleName("Regle automatique existante");
        rule.setDescription(critere.getRuleDescription());
        rule.setPriority(1);
        rule.setActive(true);
        rule.setNoteValue(5);

        switch (critere.getModeEvaluation()) {
            case DOCUMENTATION_PRESENT -> {
                rule.setMetadataKey("hasDocumentation");
                rule.setOperator(RuleOperator.EQUALS);
                rule.setExpectedValue("true");
            }
            case GIT_LINK_PRESENT -> {
                rule.setMetadataKey("hasGitLink");
                rule.setOperator(RuleOperator.EQUALS);
                rule.setExpectedValue("true");
            }
            case LIVRABLE_TYPE_PRESENT -> {
                rule.setMetadataKey("livrableTypes");
                rule.setOperator(RuleOperator.CONTAINS);
                rule.setExpectedValue(critere.getExpectedLivrableTypes());
            }
            case LIVRABLE_LINK_PRESENT -> {
                rule.setMetadataKey("externalLinkCount");
                rule.setOperator(RuleOperator.GTE);
                rule.setMinValue(1.0);
            }
            case LIVRABLE_FILE_PRESENT -> {
                rule.setMetadataKey("fileCount");
                rule.setOperator(RuleOperator.GTE);
                rule.setMinValue(1.0);
            }
            case LIVRABLE_COUNT_MIN -> {
                rule.setMetadataKey("livrableCount");
                rule.setOperator(RuleOperator.GTE);
                rule.setMinValue((double) (critere.getMinLivrableCount() == null ? 1 : critere.getMinLivrableCount()));
            }
            case KEYWORD_IN_LIVRABLE_NAME_OR_DESCRIPTION -> {
                rule.setMetadataKey("livrableSearchText");
                rule.setOperator(RuleOperator.CONTAINS);
                rule.setExpectedValue(critere.getExpectedKeyword());
            }
            case MANUAL_ADMIN_VALUE -> {
                rule.setMetadataKey("manual");
                rule.setOperator(RuleOperator.NOT_EXISTS);
            }
        }
        critereNoteRuleRepository.save(rule);
    }

    private void setRule(CritereEvaluation critere,
                         ModeEvaluation mode,
                         String expectedTypes,
                         Integer minCount,
                         String description) {
        critere.setModeEvaluation(mode);
        critere.setExpectedLivrableTypes(expectedTypes);
        critere.setMinLivrableCount(minCount);
        critere.setRuleEnabled(true);
        critere.setRuleDescription(description);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT);
    }
}
