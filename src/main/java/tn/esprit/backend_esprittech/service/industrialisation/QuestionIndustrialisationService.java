package tn.esprit.backend_esprittech.service.industrialisation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.industrialisation.QuestionIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.QuestionIndustrialisationResponse;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionIndustrialisationService {

    private final QuestionIndustrialisationRepository repository;

    @Historise(action = ActionType.CREATE, entityType = EntityType.QUESTION_INDUSTRIALISATION, entityIdExpression = "#result.id()", summaryExpression = "'Création question ' + #result.libelle()")
    public QuestionIndustrialisationResponse create(QuestionIndustrialisationRequest request) {
        validate(request);
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        apply(question, request);
        return toResponse(repository.save(question));
    }

    public Map<String, Object> getQuestionOldValues(Long id) {
        QuestionIndustrialisation q = getEntity(id);
        Map<String, Object> map = new HashMap<>();
        map.put("libelle", q.getLibelle());
        map.put("actif", q.getActif());
        map.put("ordre", q.getOrdre());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.QUESTION_INDUSTRIALISATION, entityIdExpression = "#id", oldValuesExpression = "@questionIndustrialisationService.getQuestionOldValues(#id)", summaryExpression = "'Modification question ' + #result.libelle()")
    public QuestionIndustrialisationResponse update(Long id, QuestionIndustrialisationRequest request) {
        validate(request);
        QuestionIndustrialisation question = getEntity(id);
        apply(question, request);
        return toResponse(repository.save(question));
    }

    @Historise(action = ActionType.ACTIVATE, entityType = EntityType.QUESTION_INDUSTRIALISATION, entityIdExpression = "#id", oldValuesExpression = "@questionIndustrialisationService.getQuestionOldValues(#id)", newValuesExpression = "{'actif': true}", summaryExpression = "'Activation question (id=' + #id + ')'")
    public void activate(Long id) {
        QuestionIndustrialisation question = getEntity(id);
        question.setActif(true);
        repository.save(question);
    }

    @Historise(action = ActionType.DEACTIVATE, entityType = EntityType.QUESTION_INDUSTRIALISATION, entityIdExpression = "#id", oldValuesExpression = "@questionIndustrialisationService.getQuestionOldValues(#id)", newValuesExpression = "{'actif': false}", summaryExpression = "'Désactivation question (id=' + #id + ')'")
    public void deactivate(Long id) {
        QuestionIndustrialisation question = getEntity(id);
        question.setActif(false);
        repository.save(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionIndustrialisationResponse> findAll() {
        return repository.findAllByOrderByOrdreAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionIndustrialisationResponse> findActive() {
        return repository.findByActifTrueOrderByOrdreAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private QuestionIndustrialisation getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question industrialisation introuvable avec id: " + id));
    }

    private void apply(QuestionIndustrialisation question, QuestionIndustrialisationRequest request) {
        question.setLibelle(request.libelle().trim());
        question.setDescription(trimToNull(request.description()));
        question.setTypeReponse(request.typeReponse());
        question.setObligatoire(request.obligatoire() == null || request.obligatoire());
        question.setTypeCritere(request.typeCritere());
        question.setPoids(request.poids());
        question.setOrdre(request.ordre());
        question.setActif(request.actif() == null || request.actif());
        question.setConditionEliminatoire(request.conditionEliminatoire());
    }

    private void validate(QuestionIndustrialisationRequest request) {
        if (!StringUtils.hasText(request.libelle())) {
            throw new BusinessException("Le libelle est obligatoire.");
        }
        if (request.ordre() == null || request.ordre() < 1) {
            throw new BusinessException("L'ordre doit etre superieur ou egal a 1.");
        }
        if (request.poids() != null && request.poids() < 0) {
            throw new BusinessException("Le poids ne peut pas etre negatif.");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private QuestionIndustrialisationResponse toResponse(QuestionIndustrialisation question) {
        return new QuestionIndustrialisationResponse(
                question.getId(),
                question.getLibelle(),
                question.getDescription(),
                question.getTypeReponse(),
                question.getObligatoire(),
                question.getTypeCritere(),
                question.getPoids(),
                question.getOrdre(),
                question.getActif(),
                question.getConditionEliminatoire(),
                question.getDateCreation(),
                question.getDateMiseAJour()
        );
    }
}
