package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRuleRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRuleResponse;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNoteRule;
import tn.esprit.backend_esprittech.entity.CritereEval.RuleOperator;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CritereNoteRuleService {

    private final CritereNoteRepository critereNoteRepository;
    private final CritereNoteRuleRepository ruleRepository;
    private final NoteLevelService noteLevelService;

    @Transactional(readOnly = true)
    public List<CritereNoteRuleResponse> findByCritere(Long critereId) {
        ensureCritereExists(critereId);
        return ruleRepository.findByCritereNoteIdOrderByPriorityAscIdAsc(critereId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Historise(action = ActionType.CREATE, entityType = EntityType.CRITERE_NOTE_RULE, entityIdExpression = "#result.id()", summaryExpression = "'Création règle ' + #result.ruleName()")
    public CritereNoteRuleResponse create(Long critereId, CritereNoteRuleRequest request) {
        validate(request);
        CritereNote critere = getCritere(critereId);
        ensureUniqueRuleName(critereId, request.ruleName(), null);
        CritereNoteRule rule = new CritereNoteRule();
        rule.setCritereNote(critere);
        apply(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    public Map<String, Object> getRuleOldValues(Long ruleId) {
        CritereNoteRule r = getRule(ruleId);
        Map<String, Object> map = new HashMap<>();
        map.put("ruleName", r.getRuleName());
        map.put("active", r.getActive());
        map.put("noteValue", r.getNoteValue());
        map.put("priority", r.getPriority());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.CRITERE_NOTE_RULE, entityIdExpression = "#ruleId", oldValuesExpression = "@critereNoteRuleService.getRuleOldValues(#ruleId)", summaryExpression = "'Modification règle ' + #result.ruleName()")
    public CritereNoteRuleResponse update(Long ruleId, CritereNoteRuleRequest request) {
        validate(request);
        CritereNoteRule rule = getRule(ruleId);
        ensureUniqueRuleName(rule.getCritereNote().getId(), request.ruleName(), ruleId);
        apply(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.CRITERE_NOTE_RULE, entityIdExpression = "#ruleId", oldValuesExpression = "@critereNoteRuleService.getRuleOldValues(#ruleId)", summaryExpression = "'Suppression règle (id=' + #ruleId + ')'")
    public void delete(Long ruleId) {
        ruleRepository.delete(getRule(ruleId));
    }

    @Historise(action = ActionType.ACTIVATE, entityType = EntityType.CRITERE_NOTE_RULE, entityIdExpression = "#ruleId", oldValuesExpression = "@critereNoteRuleService.getRuleOldValues(#ruleId)", newValuesExpression = "{'active': true}", summaryExpression = "'Activation règle (id=' + #ruleId + ')'")
    public void activate(Long ruleId) {
        CritereNoteRule rule = getRule(ruleId);
        rule.setActive(true);
        ruleRepository.save(rule);
    }

    @Historise(action = ActionType.DEACTIVATE, entityType = EntityType.CRITERE_NOTE_RULE, entityIdExpression = "#ruleId", oldValuesExpression = "@critereNoteRuleService.getRuleOldValues(#ruleId)", newValuesExpression = "{'active': false}", summaryExpression = "'Désactivation règle (id=' + #ruleId + ')'")
    public void deactivate(Long ruleId) {
        CritereNoteRule rule = getRule(ruleId);
        rule.setActive(false);
        ruleRepository.save(rule);
    }

    private void validate(CritereNoteRuleRequest request) {
        if (request.ruleName() == null || request.ruleName().isBlank()) {
            throw new BadRequestException("Le nom de la regle est obligatoire.");
        }
        if (request.metadataKey() == null || request.metadataKey().isBlank()) {
            throw new BadRequestException("La cle metadata est obligatoire.");
        }
        if (request.operator() == null) {
            throw new BadRequestException("L'operateur est obligatoire.");
        }
        if (request.noteValue() == null) {
            throw new BadRequestException("La note de regle est obligatoire.");
        }
        if (request.noteValue() < 1) {
            throw new BadRequestException("La note de regle doit etre superieure ou egale a 1.");
        }
        noteLevelService.requireActiveValue(request.noteValue(), "La note de regle");
        if (request.priority() == null || request.priority() < 1) {
            throw new BadRequestException("La priorite doit etre superieure ou egale a 1.");
        }
        RuleOperator operator = request.operator();
        if (operator == RuleOperator.BETWEEN
                && (request.minValue() == null || request.maxValue() == null)) {
            throw new BadRequestException("Les valeurs min et max sont obligatoires pour BETWEEN.");
        }
        if (operator == RuleOperator.BETWEEN && request.minValue() > request.maxValue()) {
            throw new BadRequestException("La valeur min doit etre inferieure ou egale a la valeur max.");
        }
        if ((operator == RuleOperator.GTE || operator == RuleOperator.LTE
                || operator == RuleOperator.COUNT_GTE || operator == RuleOperator.COUNT_LTE)
                && request.minValue() == null
                && (request.expectedValue() == null || request.expectedValue().isBlank())) {
            throw new BadRequestException("Une valeur numerique est obligatoire pour cet operateur.");
        }
    }

    private void ensureCritereExists(Long critereId) {
        if (!critereNoteRepository.existsById(critereId)) {
            throw new ResourceNotFoundException("Critere note introuvable avec id: " + critereId);
        }
    }

    private CritereNote getCritere(Long critereId) {
        return critereNoteRepository.findById(critereId)
                .orElseThrow(() -> new ResourceNotFoundException("Critere note introuvable avec id: " + critereId));
    }

    private CritereNoteRule getRule(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Regle de critere note introuvable avec id: " + ruleId));
    }

    private void apply(CritereNoteRule rule, CritereNoteRuleRequest request) {
        rule.setRuleName(request.ruleName().trim());
        rule.setDescription(trimToNull(request.description()));
        rule.setMetadataKey(request.metadataKey().trim());
        rule.setOperator(request.operator());
        rule.setExpectedValue(trimToNull(request.expectedValue()));
        rule.setMinValue(request.minValue());
        rule.setMaxValue(request.maxValue());
        rule.setNoteValue(request.noteValue());
        rule.setPriority(request.priority());
        if (request.active() != null) {
            rule.setActive(request.active());
        }
    }

    private void ensureUniqueRuleName(Long critereId, String ruleName, Long currentRuleId) {
        ruleRepository.findByCritereNoteIdAndRuleNameIgnoreCase(critereId, ruleName.trim())
                .filter(existing -> currentRuleId == null || !existing.getId().equals(currentRuleId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Une regle existe deja avec le nom: " + ruleName.trim());
                });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private CritereNoteRuleResponse toResponse(CritereNoteRule rule) {
        return new CritereNoteRuleResponse(
                rule.getId(),
                rule.getCritereNote().getId(),
                rule.getRuleName(),
                rule.getDescription(),
                rule.getMetadataKey(),
                rule.getOperator(),
                rule.getExpectedValue(),
                rule.getMinValue(),
                rule.getMaxValue(),
                rule.getNoteValue(),
                noteLevelService.labelFor(rule.getNoteValue()),
                rule.getPriority(),
                rule.getActive(),
                rule.getDateCreation(),
                rule.getDateMiseAJour()
        );
    }
}
