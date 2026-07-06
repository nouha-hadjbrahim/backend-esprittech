package tn.esprit.backend_esprittech.service.CritereEval;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereEliminatoireResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CritereEliminatoireService {

    private final CritereEliminatoireRepository repository;

    @Historise(action = ActionType.CREATE, entityType = EntityType.CRITERE_ELIMINATOIRE, entityIdExpression = "#result.id()", summaryExpression = "'Création critère éliminatoire ' + #result.libelle()")
    public CritereEliminatoireResponse create(CritereEliminatoireRequest request) {
        validateRequest(request);

        CritereEliminatoire critere = new CritereEliminatoire();
        applyRequest(critere, request);

        return toResponse(repository.save(critere));
    }

    public Map<String, Object> getCritereEliminatoireOldValues(Long id) {
        CritereEliminatoire c = getEntity(id);
        Map<String, Object> map = new HashMap<>();
        map.put("libelle", c.getLibelle());
        map.put("actif", c.getActif());
        map.put("ordre", c.getOrdre());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.CRITERE_ELIMINATOIRE, entityIdExpression = "#id", oldValuesExpression = "@critereEliminatoireService.getCritereEliminatoireOldValues(#id)", summaryExpression = "'Modification critère éliminatoire ' + #result.libelle()")
    public CritereEliminatoireResponse update(Long id, CritereEliminatoireRequest request) {
        validateRequest(request);

        CritereEliminatoire critere = getEntity(id);
        applyRequest(critere, request);

        return toResponse(repository.save(critere));
    }

    @Historise(action = ActionType.DEACTIVATE, entityType = EntityType.CRITERE_ELIMINATOIRE, entityIdExpression = "#id", oldValuesExpression = "@critereEliminatoireService.getCritereEliminatoireOldValues(#id)", newValuesExpression = "{'actif': false}", summaryExpression = "'Désactivation critère éliminatoire (id=' + #id + ')'")
    public void deactivate(Long id) {
        CritereEliminatoire critere = getEntity(id);
        critere.setActif(false);
        repository.save(critere);
    }

    @Historise(action = ActionType.ACTIVATE, entityType = EntityType.CRITERE_ELIMINATOIRE, entityIdExpression = "#id", oldValuesExpression = "@critereEliminatoireService.getCritereEliminatoireOldValues(#id)", newValuesExpression = "{'actif': true}", summaryExpression = "'Activation critère éliminatoire (id=' + #id + ')'")
    public void activate(Long id) {
        CritereEliminatoire critere = getEntity(id);
        critere.setActif(true);
        repository.save(critere);
    }

    @Historise(action = ActionType.REORDER, entityType = EntityType.CRITERE_ELIMINATOIRE, entityIdExpression = "#requests[0].critereId()", oldValuesExpression = "@critereEliminatoireService.getCritereEliminatoireOldValues(#requests[0].critereId())", summaryExpression = "'Réordonnancement de ' + #requests.size() + ' critères éliminatoires'")
    public void reorder(List<OrdreCritereRequest> requests) {
        for (OrdreCritereRequest request : requests) {
            CritereEliminatoire critere = getEntity(request.critereId());

            if (request.ordre() == null || request.ordre() < 1) {
                throw new BadRequestException("L'ordre doit être supérieur ou égal à 1.");
            }

            critere.setOrdre(request.ordre());
            repository.save(critere);
        }
    }

    @Transactional(readOnly = true)
    public List<CritereEliminatoireResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CritereEliminatoireResponse> findActive() {
        return repository.findByActifTrueOrderByOrdreAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CritereEliminatoire getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Critère éliminatoire introuvable avec id: " + id));
    }

    private void applyRequest(CritereEliminatoire critere, CritereEliminatoireRequest request) {
        critere.setLibelle(request.libelle());
        critere.setDescription(request.description());
        critere.setDomaine(request.domaine());
        critere.setOrdre(request.ordre());
        critere.setReponseAttendue(request.reponseAttendue());
        critere.setModeEvaluation(request.modeEvaluation());
        critere.setExpectedLivrableTypes(request.expectedLivrableTypes());
        critere.setMinLivrableCount(request.minLivrableCount());
        critere.setExpectedKeyword(request.expectedKeyword());
        critere.setNoteMaxAuto(request.noteMaxAuto());
        critere.setRuleDescription(request.ruleDescription());

        if (request.actif() != null) {
            critere.setActif(request.actif());
        }
        critere.setRuleEnabled(Boolean.TRUE.equals(request.ruleEnabled()));
    }

    private void validateRequest(CritereEliminatoireRequest request) {
        if (request.ordre() == null || request.ordre() < 1) {
            throw new BadRequestException("L'ordre doit être supérieur ou égal à 1.");
        }

        if (request.libelle() == null || request.libelle().isBlank()) {
            throw new BadRequestException("Le libellé est obligatoire.");
        }
    }

    private CritereEliminatoireResponse toResponse(CritereEliminatoire critere) {
        return new CritereEliminatoireResponse(
                critere.getId(),
                critere.getLibelle(),
                critere.getDescription(),
                critere.getDomaine(),
                critere.getOrdre(),
                critere.getActif(),
                critere.getDateCreation(),
                critere.getDateMiseAJour(),
                critere.getReponseAttendue(),
                critere.getModeEvaluation(),
                critere.getExpectedLivrableTypes(),
                critere.getMinLivrableCount(),
                critere.getExpectedKeyword(),
                critere.getNoteMaxAuto(),
                critere.getRuleEnabled(),
                critere.getRuleDescription()
        );
    }
}
