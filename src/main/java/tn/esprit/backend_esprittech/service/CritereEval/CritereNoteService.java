package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.ResultatCritereRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CritereNoteService {

    private static final String USED_CRITERE_DELETE_MESSAGE =
            "Ce critère est déjà utilisé dans des évaluations. Vous pouvez le désactiver au lieu de le supprimer.";

    private final CritereNoteRepository repository;
    private final CritereNoteRuleRepository ruleRepository;
    private final ResultatCritereRepository resultatCritereRepository;
    private final NoteLevelService noteLevelService;

    @Historise(action = ActionType.CREATE, entityType = EntityType.CRITERE_NOTE, entityIdExpression = "#result.id()", summaryExpression = "'Création critère note ' + #result.libelle()")
    public CritereNoteResponse create(CritereNoteRequest request) {
        validateRequest(request);
        ensureUniqueLibelle(request.libelle(), null);
        CritereNote critere = new CritereNote();
        applyRequest(critere, request);
        return toResponse(repository.save(critere));
    }

    public Map<String, Object> getOldValues(Long id) {
        CritereNote c = getEntity(id);
        Map<String, Object> map = new HashMap<>();
        map.put("libelle", c.getLibelle());
        map.put("actif", c.getActif());
        map.put("ordre", c.getOrdre());
        map.put("domaine", c.getDomaine());
        map.put("bareme", c.getBareme());
        map.put("poids", c.getPoids());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.CRITERE_NOTE, entityIdExpression = "#id", oldValuesExpression = "@critereNoteService.getOldValues(#id)", summaryExpression = "'Modification critère note ' + #result.libelle()")
    public CritereNoteResponse update(Long id, CritereNoteRequest request) {
        validateRequest(request);
        CritereNote critere = getEntity(id);
        ensureUniqueLibelle(request.libelle(), id);
        applyRequest(critere, request);
        return toResponse(repository.save(critere));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.CRITERE_NOTE, entityIdExpression = "#id", oldValuesExpression = "@critereNoteService.getOldValues(#id)", summaryExpression = "'Suppression critère note (id=' + #id + ')'")
    public void delete(Long id) {
        CritereNote critere = getEntity(id);
        if (resultatCritereRepository.existsByCritereId(id)) {
            throw new BusinessConflictException(USED_CRITERE_DELETE_MESSAGE);
        }
        ruleRepository.deleteAll(ruleRepository.findByCritereNoteIdOrderByPriorityAscIdAsc(id));
        repository.delete(critere);
    }

    @Historise(action = ActionType.DEACTIVATE, entityType = EntityType.CRITERE_NOTE, entityIdExpression = "#id", oldValuesExpression = "@critereNoteService.getOldValues(#id)", newValuesExpression = "{'actif': false}", summaryExpression = "'Désactivation critère note (id=' + #id + ')'")
    public void deactivate(Long id) {
        CritereNote critere = getEntity(id);
        critere.setActif(false);
        repository.save(critere);
    }

    @Historise(action = ActionType.ACTIVATE, entityType = EntityType.CRITERE_NOTE, entityIdExpression = "#id", oldValuesExpression = "@critereNoteService.getOldValues(#id)", newValuesExpression = "{'actif': true}", summaryExpression = "'Activation critère note (id=' + #id + ')'")
    public void activate(Long id) {
        CritereNote critere = getEntity(id);
        critere.setActif(true);
        repository.save(critere);
    }

    @Historise(action = ActionType.REORDER, entityType = EntityType.CRITERE_NOTE, entityIdExpression = "#requests[0].critereId()", oldValuesExpression = "@critereNoteService.getOldValues(#requests[0].critereId())", summaryExpression = "'Réordonnancement de ' + #requests.size() + ' critères note'")
    public void reorder(List<OrdreCritereRequest> requests) {
        for (OrdreCritereRequest request : requests) {
            CritereNote critere = getEntity(request.critereId());
            if (request.ordre() == null || request.ordre() < 1) {
                throw new BadRequestException("L'ordre doit etre superieur ou egal a 1.");
            }
            critere.setOrdre(request.ordre());
            repository.save(critere);
        }
    }

    @Transactional(readOnly = true)
    public List<CritereNoteResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CritereNoteResponse> findActive() {
        return repository.findByActifTrueOrderByOrdreAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CritereNote getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Critere note introuvable avec id: " + id));
    }

    private void applyRequest(CritereNote critere, CritereNoteRequest request) {
        critere.setLibelle(request.libelle().trim());
        critere.setDescription(trimToNull(request.description()));
        critere.setDomaine(request.domaine().trim());
        critere.setOrdre(request.ordre());
        critere.setBareme(request.bareme() == null ? 5.0 : request.bareme());
        critere.setPoids(request.poids());
        critere.setSeuil(request.seuil() == null ? 3.0 : request.seuil());
        critere.setDefaultNoteValue(request.defaultNoteValue() == null ? 3 : request.defaultNoteValue());
        critere.setModeEvaluation(request.modeEvaluation());
        critere.setExpectedLivrableTypes(trimToNull(request.expectedLivrableTypes()));
        critere.setMinLivrableCount(request.minLivrableCount());
        critere.setExpectedKeyword(trimToNull(request.expectedKeyword()));
        critere.setNoteMaxAuto(request.noteMaxAuto());
        critere.setRuleDescription(trimToNull(request.ruleDescription()));

        if (request.actif() != null) {
            critere.setActif(request.actif());
        }
        critere.setRuleEnabled(Boolean.TRUE.equals(request.ruleEnabled()));
    }

    private void validateRequest(CritereNoteRequest request) {
        if (request.libelle() == null || request.libelle().isBlank()) {
            throw new BadRequestException("Le libelle est obligatoire.");
        }
        if (request.domaine() == null || request.domaine().isBlank()) {
            throw new BadRequestException("Le domaine est obligatoire.");
        }
        if (request.ordre() == null || request.ordre() < 1) {
            throw new BadRequestException("L'ordre doit etre superieur ou egal a 1.");
        }
        if (request.bareme() != null && request.bareme() <= 0) {
            throw new BadRequestException("Le bareme doit etre superieur a 0.");
        }
        if (request.poids() == null || request.poids() <= 0) {
            throw new BadRequestException("Le poids doit etre superieur a 0.");
        }
        if (request.seuil() != null && request.seuil() < 0) {
            throw new BadRequestException("Le seuil doit etre positif.");
        }
        if (request.defaultNoteValue() != null) {
            noteLevelService.requireActiveValue(request.defaultNoteValue(), "La note par defaut");
        }
        if (request.minLivrableCount() != null && request.minLivrableCount() < 1) {
            throw new BadRequestException("Le nombre minimum de livrables doit etre superieur ou egal a 1.");
        }
        if (request.noteMaxAuto() != null && request.noteMaxAuto() <= 0) {
            throw new BadRequestException("La note maximale automatique doit etre superieure a 0.");
        }
    }

    private void ensureUniqueLibelle(String libelle, Long currentId) {
        repository.findByLibelleIgnoreCase(libelle.trim())
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Un critere note existe deja avec le libelle: " + libelle.trim());
                });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private CritereNoteResponse toResponse(CritereNote critere) {
        return new CritereNoteResponse(
                critere.getId(),
                critere.getLibelle(),
                critere.getDescription(),
                critere.getDomaine(),
                critere.getOrdre(),
                critere.getActif(),
                critere.getDateCreation(),
                critere.getDateMiseAJour(),
                critere.getBareme(),
                critere.getPoids(),
                critere.getSeuil(),
                critere.getDefaultNoteValue(),
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
