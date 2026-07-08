package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRuleRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNoteRule;
import tn.esprit.backend_esprittech.entity.CritereEval.RuleOperator;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CritereNoteRuleServiceTest {

    @Mock
    private CritereNoteRepository critereNoteRepository;

    @Mock
    private CritereNoteRuleRepository ruleRepository;

    @Mock
    private NoteLevelService noteLevelService;

    @InjectMocks
    private CritereNoteRuleService service;

    @Test
    void create_valueSeven_shouldSaveWhenLevelIsActive() {
        CritereNote critere = new CritereNote();
        critere.setId(1L);
        critere.setLibelle("Qualite");
        when(critereNoteRepository.findById(1L)).thenReturn(Optional.of(critere));
        when(ruleRepository.findByCritereNoteIdAndRuleNameIgnoreCase(1L, "Score 7")).thenReturn(Optional.empty());
        when(noteLevelService.labelFor(7)).thenReturn("Bon niveau");
        when(ruleRepository.save(any(CritereNoteRule.class))).thenAnswer(invocation -> {
            CritereNoteRule saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var response = service.create(1L, new CritereNoteRuleRequest(
                "Score 7",
                "Note dynamique",
                "livrableCount",
                RuleOperator.GTE,
                null,
                2.0,
                null,
                7,
                1,
                true
        ));

        assertEquals(7, response.noteValue());
        assertEquals("Bon niveau", response.noteLabel());
        verify(noteLevelService).requireActiveValue(7, "La note de regle");
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsRuleFields() {
        CritereNoteRule rule = new CritereNoteRule();
        rule.setId(5L);
        rule.setRuleName("Score 5");
        rule.setActive(true);
        rule.setNoteValue(5);
        rule.setPriority(1);
        when(ruleRepository.findById(5L)).thenReturn(Optional.of(rule));

        var result = service.getOldValues(5L);

        assertEquals("Score 5", result.get("ruleName"));
        assertEquals(true, result.get("active"));
        assertEquals(5, result.get("noteValue"));
        assertEquals(1, result.get("priority"));
    }
}
