package tn.esprit.backend_esprittech.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;

import java.text.Normalizer;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class EvaluationRuleInitializer implements CommandLineRunner {

    private final CritereEliminatoireRepository critereEliminatoireRepository;
    private final CritereNoteRepository critereNoteRepository;

    @Override
    @Transactional
    public void run(String... args) {
        critereEliminatoireRepository.findAll().forEach(critere -> {
            if (critere.getModeEvaluation() == null && applyDefaultRule(critere)) {
                critereEliminatoireRepository.save(critere);
            }
        });
        critereNoteRepository.findAll().forEach(critere -> {
            if (critere.getModeEvaluation() == null && applyDefaultRule(critere)) {
                if (critere.getNoteMaxAuto() == null) {
                    critere.setNoteMaxAuto(critere.getBareme());
                }
                critereNoteRepository.save(critere);
            }
        });
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
