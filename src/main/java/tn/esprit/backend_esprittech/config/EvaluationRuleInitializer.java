package tn.esprit.backend_esprittech.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.NoteLevel;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.NoteLevelRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaluationRuleInitializer implements CommandLineRunner {

    private final CritereNoteRepository critereNoteRepository;
    private final NoteLevelRepository noteLevelRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedNoteLevels();
        critereNoteRepository.findAll().forEach(critere -> {
            if (applyNoteDefaults(critere)) {
                critereNoteRepository.save(critere);
            }
        });
    }

    private void seedNoteLevels() {
        if (noteLevelRepository.count() > 0) {
            return;
        }
        List<String> labels = List.of(
                "Tres insatisfait",
                "Insatisfait",
                "Peu satisfait",
                "Satisfait",
                "Tres satisfait"
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

    private boolean applyNoteDefaults(CritereNote critere) {
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
        return changed;
    }
}
