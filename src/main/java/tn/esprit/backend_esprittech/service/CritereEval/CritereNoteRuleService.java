package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRuleRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRuleResponse;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNoteRule;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;

import java.util.List;

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

    public CritereNoteRuleResponse create(Long critereId, CritereNoteRuleRequest request) {
        validate(request);
        CritereNote critere = getCritere(critereId);
        CritereNoteRule rule = new CritereNoteRule();
        rule.setCritereNote(critere);
        apply(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    public CritereNoteRuleResponse update(Long ruleId, CritereNoteRuleRequest request) {
        validate(request);
        CritereNoteRule rule = getRule(ruleId);
        apply(rule, request);
        return toResponse(ruleRepository.save(rule));
    }

    public void delete(Long ruleId) {
        ruleRepository.delete(getRule(ruleId));
    }

    public void activate(Long ruleId) {
        CritereNoteRule rule = getRule(ruleId);
        rule.setActive(true);
        ruleRepository.save(rule);
    }

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
        if (request.noteValue() == null || request.noteValue() < 1 || request.noteValue() > 5) {
            throw new BadRequestException("La note de regle doit etre comprise entre 1 et 5.");
        }
        if (request.priority() == null || request.priority() < 1) {
            throw new BadRequestException("La priorite doit etre superieure ou egale a 1.");
        }
        if (request.operator().name().equals("BETWEEN")
                && (request.minValue() == null || request.maxValue() == null)) {
            throw new BadRequestException("Les valeurs min et max sont obligatoires pour BETWEEN.");
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
        rule.setDescription(request.description());
        rule.setMetadataKey(request.metadataKey().trim());
        rule.setOperator(request.operator());
        rule.setExpectedValue(request.expectedValue());
        rule.setMinValue(request.minValue());
        rule.setMaxValue(request.maxValue());
        rule.setNoteValue(request.noteValue());
        rule.setPriority(request.priority());
        if (request.active() != null) {
            rule.setActive(request.active());
        }
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
