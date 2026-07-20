package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNote;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.critereeval.NoteLevelRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationRuleInitializerTest {

    @Mock private CritereNoteRepository critereNoteRepository;
    @Mock private NoteLevelRepository noteLevelRepository;

    private EvaluationRuleInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new EvaluationRuleInitializer(critereNoteRepository, noteLevelRepository);
    }

    @Test
    void run_shouldSeedNoteLevelsAndApplyOnlyNoteDefaults() {
        CritereNote missingDefaults = note(null, 0.0, null);
        CritereNote configured = note(4, 10.0, 6.0);

        when(noteLevelRepository.count()).thenReturn(0L);
        when(critereNoteRepository.findAll()).thenReturn(List.of(missingDefaults, configured));

        initializer.run();

        verify(noteLevelRepository, times(5)).save(any());
        verify(critereNoteRepository).save(missingDefaults);
        verify(critereNoteRepository, never()).save(configured);
        assertEquals(3, missingDefaults.getDefaultNoteValue());
        assertEquals(5.0, missingDefaults.getBareme());
        assertEquals(3.0, missingDefaults.getSeuil());
        assertEquals(4, configured.getDefaultNoteValue());
        assertEquals(10.0, configured.getBareme());
        assertEquals(6.0, configured.getSeuil());
        assertNull(missingDefaults.getModeEvaluation());
    }

    private CritereNote note(Integer defaultNote, Double bareme, Double seuil) {
        CritereNote critere = new CritereNote();
        critere.setDefaultNoteValue(defaultNote);
        critere.setBareme(bareme);
        critere.setSeuil(seuil);
        return critere;
    }
}
