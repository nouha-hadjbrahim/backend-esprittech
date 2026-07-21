package tn.esprit.backend_esprittech.service.critereeval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteRuleRequest;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNote;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNoteRule;
import tn.esprit.backend_esprittech.entity.critereeval.RuleOperator;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.critereeval.BadRequestException;
import tn.esprit.backend_esprittech.exception.critereeval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRuleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    // ---- validate() ----

    @Test
    void create_blankRuleName_shouldReject() {
        var request = request("  ", "livrableCount", RuleOperator.GTE, null, 2.0, null, 7, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(1L, request));

        assertEquals("Le nom de la regle est obligatoire.", ex.getMessage());
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void create_blankMetadataKey_shouldReject() {
        var request = request("Score", " ", RuleOperator.GTE, null, 2.0, null, 7, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(1L, request));

        assertEquals("La cle metadata est obligatoire.", ex.getMessage());
    }

    @Test
    void create_noteBelowOne_shouldReject() {
        var request = request("Score", "key", RuleOperator.GTE, null, 2.0, null, 0, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(1L, request));

        assertEquals("La note de regle doit etre superieure ou egale a 1.", ex.getMessage());
    }

    @Test
    void create_priorityBelowOne_shouldReject() {
        var request = request("Score", "key", RuleOperator.GTE, null, 2.0, null, 7, 0);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(1L, request));

        assertEquals("La priorite doit etre superieure ou egale a 1.", ex.getMessage());
    }

    @Test
    void create_betweenWithoutBounds_shouldReject() {
        var request = request("Plage", "key", RuleOperator.BETWEEN, null, null, null, 7, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(1L, request));

        assertEquals("Les valeurs min et max sont obligatoires pour BETWEEN.", ex.getMessage());
    }

    @Test
    void create_betweenWithInvertedBounds_shouldReject() {
        var request = request("Plage", "key", RuleOperator.BETWEEN, null, 9.0, 2.0, 7, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(1L, request));

        assertEquals("La valeur min doit etre inferieure ou egale a la valeur max.", ex.getMessage());
    }

    @Test
    void create_comparisonOperatorWithoutValue_shouldReject() {
        var request = request("Seuil", "key", RuleOperator.COUNT_GTE, null, null, null, 7, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(1L, request));

        assertEquals("Une valeur numerique est obligatoire pour cet operateur.", ex.getMessage());
    }

    @Test
    void create_duplicateRuleName_shouldReportConflict() {
        CritereNote critere = critere();
        CritereNoteRule existing = rule(99L, critere);
        when(critereNoteRepository.findById(1L)).thenReturn(Optional.of(critere));
        when(ruleRepository.findByCritereNoteIdAndRuleNameIgnoreCase(1L, "Score"))
                .thenReturn(Optional.of(existing));
        var request = request("Score", "key", RuleOperator.GTE, null, 2.0, null, 7, 1);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> service.create(1L, request));

        assertEquals("Une regle existe deja avec le nom: Score", ex.getMessage());
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void create_unknownCritere_shouldReportNotFound() {
        when(critereNoteRepository.findById(404L)).thenReturn(Optional.empty());
        var request = request("Score", "key", RuleOperator.GTE, null, 2.0, null, 7, 1);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.create(404L, request));

        assertEquals("Critere note introuvable avec id: 404", ex.getMessage());
    }

    // ---- update / delete / activate / deactivate ----

    @Test
    void update_shouldApplyNewValuesAndKeepSameRule() {
        CritereNote critere = critere();
        CritereNoteRule existing = rule(10L, critere);
        when(ruleRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(ruleRepository.findByCritereNoteIdAndRuleNameIgnoreCase(1L, "Score revise"))
                .thenReturn(Optional.of(existing));
        when(noteLevelService.labelFor(5)).thenReturn("Moyen");
        when(ruleRepository.save(any(CritereNoteRule.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.update(10L, request("Score revise", "autreCle",
                RuleOperator.LTE, null, 4.0, null, 5, 3));

        assertEquals(10L, response.id());
        assertEquals("Score revise", response.ruleName());
        assertEquals("autreCle", response.metadataKey());
        assertEquals(5, response.noteValue());
        assertEquals(3, response.priority());
    }

    @Test
    void update_nameTakenByAnotherRule_shouldReportConflict() {
        CritereNote critere = critere();
        when(ruleRepository.findById(10L)).thenReturn(Optional.of(rule(10L, critere)));
        when(ruleRepository.findByCritereNoteIdAndRuleNameIgnoreCase(1L, "Score"))
                .thenReturn(Optional.of(rule(11L, critere)));
        var request = request("Score", "key", RuleOperator.GTE, null, 2.0, null, 7, 1);

        assertThrows(BusinessConflictException.class, () -> service.update(10L, request));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void update_unknownRule_shouldReportNotFound() {
        when(ruleRepository.findById(404L)).thenReturn(Optional.empty());
        var request = request("Score", "key", RuleOperator.GTE, null, 2.0, null, 7, 1);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.update(404L, request));

        assertEquals("Regle de critere note introuvable avec id: 404", ex.getMessage());
    }

    @Test
    void delete_shouldRemoveRule() {
        CritereNoteRule existing = rule(10L, critere());
        when(ruleRepository.findById(10L)).thenReturn(Optional.of(existing));

        service.delete(10L);

        verify(ruleRepository).delete(existing);
    }

    @Test
    void activateAndDeactivate_shouldPersistNewState() {
        CritereNoteRule existing = rule(10L, critere());
        when(ruleRepository.findById(10L)).thenReturn(Optional.of(existing));

        service.deactivate(10L);
        assertFalse(existing.getActive());

        service.activate(10L);
        assertTrue(existing.getActive());

        verify(ruleRepository, times(2)).save(existing);
    }

    // ---- findByCritere ----

    @Test
    void findByCritere_shouldReturnMappedRules() {
        CritereNote critere = critere();
        when(critereNoteRepository.existsById(1L)).thenReturn(true);
        when(ruleRepository.findByCritereNoteIdOrderByPriorityAscIdAsc(1L))
                .thenReturn(List.of(rule(10L, critere)));
        when(noteLevelService.labelFor(7)).thenReturn("Bon");

        var rules = service.findByCritere(1L);

        assertEquals(1, rules.size());
        assertEquals(10L, rules.get(0).id());
        assertEquals("Bon", rules.get(0).noteLabel());
    }

    @Test
    void findByCritere_unknownCritere_shouldReportNotFound() {
        when(critereNoteRepository.existsById(404L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.findByCritere(404L));

        assertEquals("Critere note introuvable avec id: 404", ex.getMessage());
    }

    private CritereNoteRuleRequest request(String nom, String metadataKey, RuleOperator operator,
                                           String expectedValue, Double min, Double max,
                                           Integer note, Integer priorite) {
        return new CritereNoteRuleRequest(nom, "Description", metadataKey, operator,
                expectedValue, min, max, note, priorite, true);
    }

    private CritereNote critere() {
        CritereNote critere = new CritereNote();
        critere.setId(1L);
        critere.setLibelle("Qualite");
        return critere;
    }

    private CritereNoteRule rule(Long id, CritereNote critere) {
        CritereNoteRule rule = new CritereNoteRule();
        rule.setId(id);
        rule.setCritereNote(critere);
        rule.setRuleName("Score");
        rule.setMetadataKey("livrableCount");
        rule.setOperator(RuleOperator.GTE);
        rule.setMinValue(2.0);
        rule.setNoteValue(7);
        rule.setPriority(1);
        rule.setActive(true);
        return rule;
    }
}
