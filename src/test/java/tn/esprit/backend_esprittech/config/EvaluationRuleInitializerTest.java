package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.NoteLevel;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.NoteLevelRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationRuleInitializerTest {

    @Mock
    private CritereEliminatoireRepository critereEliminatoireRepository;

    @Mock
    private CritereNoteRepository critereNoteRepository;

    @Mock
    private CritereNoteRuleRepository critereNoteRuleRepository;

    @Mock
    private NoteLevelRepository noteLevelRepository;

    private EvaluationRuleInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new EvaluationRuleInitializer(
                critereEliminatoireRepository,
                critereNoteRepository,
                critereNoteRuleRepository,
                noteLevelRepository
        );
    }

    @Test
    void run_shouldApplyDefaultRulesForKnownLabelsAndSkipUnknownOrConfiguredCriteria() throws Exception {
        CritereEliminatoire documentation = eliminatoire("Documentation livrable");
        CritereEliminatoire git = eliminatoire("Lien Git obligatoire");
        CritereEliminatoire nullLabel = eliminatoire(null);

        CritereNote rapport = note("Rapport final", 10.0);
        CritereNote presentation = note("Presentation client", 5.0);
        presentation.setNoteMaxAuto(3.0);
        CritereNote richesse = note("Richesse des livrables", 8.0);
        CritereNote qualite = note("Qualite technique", 6.0);
        CritereNote unknown = note("Impact metier", 4.0);
        CritereNote alreadyConfigured = note("Documentation deja configuree", 2.0);
        alreadyConfigured.setModeEvaluation(ModeEvaluation.MANUAL_ADMIN_VALUE);

        when(critereEliminatoireRepository.findAll()).thenReturn(List.of(documentation, git, nullLabel));
        when(critereNoteRepository.findAll()).thenReturn(List.of(
                rapport,
                presentation,
                richesse,
                qualite,
                unknown,
                alreadyConfigured
        ));
        when(noteLevelRepository.count()).thenReturn(5L);
        when(noteLevelRepository.findTopByActiveTrueOrderByValueDesc()).thenReturn(Optional.of(noteLevel(5)));
        when(critereNoteRuleRepository.existsByCritereNoteId(any())).thenReturn(false);

        initializer.run();

        assertEquals(ModeEvaluation.DOCUMENTATION_PRESENT, documentation.getModeEvaluation());
        assertEquals("DOCUMENTATION,RAPPORT", documentation.getExpectedLivrableTypes());
        assertEquals(1, documentation.getMinLivrableCount());
        assertTrue(documentation.getRuleEnabled());

        assertEquals(ModeEvaluation.GIT_LINK_PRESENT, git.getModeEvaluation());
        assertEquals("LIEN_GIT,CODE_SOURCE", git.getExpectedLivrableTypes());

        assertEquals(ModeEvaluation.LIVRABLE_TYPE_PRESENT, rapport.getModeEvaluation());
        assertEquals("RAPPORT", rapport.getExpectedLivrableTypes());
        assertNull(rapport.getNoteMaxAuto());

        assertEquals(ModeEvaluation.LIVRABLE_TYPE_PRESENT, presentation.getModeEvaluation());
        assertEquals("PRESENTATION", presentation.getExpectedLivrableTypes());
        assertEquals(3.0, presentation.getNoteMaxAuto());

        assertEquals(ModeEvaluation.LIVRABLE_COUNT_MIN, richesse.getModeEvaluation());
        assertNull(richesse.getExpectedLivrableTypes());
        assertEquals(3, richesse.getMinLivrableCount());

        assertEquals(ModeEvaluation.GIT_LINK_PRESENT, qualite.getModeEvaluation());
        assertEquals("LIEN_GIT,CODE_SOURCE", qualite.getExpectedLivrableTypes());

        assertNull(nullLabel.getModeEvaluation());
        assertNull(unknown.getModeEvaluation());
        assertEquals(ModeEvaluation.MANUAL_ADMIN_VALUE, alreadyConfigured.getModeEvaluation());
        verify(critereEliminatoireRepository, times(2)).save(any(CritereEliminatoire.class));
        verify(critereNoteRepository, times(4)).save(any(CritereNote.class));
        verify(critereNoteRuleRepository, times(4)).save(any());
        verify(critereNoteRepository, never()).save(unknown);
        verify(critereNoteRepository, never()).save(alreadyConfigured);
    }

    private CritereEliminatoire eliminatoire(String libelle) {
        CritereEliminatoire critere = new CritereEliminatoire();
        critere.setLibelle(libelle);
        return critere;
    }

    private CritereNote note(String libelle, Double bareme) {
        CritereNote critere = new CritereNote();
        critere.setLibelle(libelle);
        critere.setBareme(bareme);
        return critere;
    }

    private NoteLevel noteLevel(Integer value) {
        NoteLevel level = new NoteLevel();
        level.setValue(value);
        level.setLabel("Niveau " + value);
        level.setOrderIndex(value);
        level.setActive(true);
        return level;
    }
}
