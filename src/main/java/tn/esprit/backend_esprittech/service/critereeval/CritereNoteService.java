package tn.esprit.backend_esprittech.service.critereeval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteResponse;
import tn.esprit.backend_esprittech.dto.critereeval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNote;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.critereeval.BadRequestException;
import tn.esprit.backend_esprittech.exception.critereeval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.critereeval.ResultatCritereRepository;

import java.util.List;

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
    private final CriterionConfigurationPolicy configurationPolicy;

    public CritereNoteResponse create(CritereNoteRequest request) {
        validateRequest(request);
        ensureUniqueLibelle(request.libelle(), null);
        CritereNote critere = new CritereNote();
        applyRequest(critere, request);
        configurationPolicy.requireActiveComplete(critere, TypeCritere.NOTE);
        return toResponse(repository.save(critere));
    }

    public CritereNoteResponse update(Long id, CritereNoteRequest request) {
        validateRequest(request);
        CritereNote critere = getEntity(id);
        ensureUniqueLibelle(request.libelle(), id);
        applyRequest(critere, request);
        configurationPolicy.requireActiveComplete(critere, TypeCritere.NOTE);
        return toResponse(repository.save(critere));
    }

    public void delete(Long id) {
        CritereNote critere = getEntity(id);
        if (resultatCritereRepository.existsByCritereId(id)) {
            throw new BusinessConflictException(USED_CRITERE_DELETE_MESSAGE);
        }
        ruleRepository.deleteAll(ruleRepository.findByCritereNoteIdOrderByPriorityAscIdAsc(id));
        repository.delete(critere);
    }

    public void deactivate(Long id) {
        CritereNote critere = getEntity(id);
        critere.setActif(false);
        repository.save(critere);
    }

    public void activate(Long id) {
        CritereNote critere = getEntity(id);
        critere.setActif(true);
        configurationPolicy.requireActiveComplete(critere, TypeCritere.NOTE);
        repository.save(critere);
    }

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
        applyConfigurationFields(critere, request);

        if (request.actif() != null) {
            critere.setActif(request.actif());
        }
        critere.setRuleEnabled(Boolean.TRUE.equals(request.ruleEnabled()));
    }

    private void applyConfigurationFields(CritereNote critere, CritereNoteRequest request) {
        critere.setCriterionFamily(trimToNull(request.criterionFamily()));
        critere.setConfigurationVersion(trimToNull(request.configurationVersion()));
        critere.setApplicability(trimToNull(request.applicability()));
        critere.setRequiredEvidenceTypes(trimToNull(request.requiredEvidenceTypes()));
        critere.setOptionalEvidenceTypes(trimToNull(request.optionalEvidenceTypes()));
        critere.setPositiveEvidenceExpectations(trimToNull(request.positiveEvidenceExpectations()));
        critere.setNegativeEvidenceExpectations(trimToNull(request.negativeEvidenceExpectations()));
        critere.setPermittedEvidenceNatures(trimToNull(request.permittedEvidenceNatures()));
        critere.setMinimumSupportingEvidenceCount(request.minimumSupportingEvidenceCount());
        critere.setScoringPolicy(trimToNull(request.scoringPolicy()));
        critere.setEliminatoryDecisionRule(trimToNull(request.eliminatoryDecisionRule()));
        critere.setSourceAuthorityRules(trimToNull(request.sourceAuthorityRules()));
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
        CriterionConfigurationPolicy.ConfigurationReport report =
                configurationPolicy.inspect(critere, TypeCritere.NOTE);
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
                critere.getRuleDescription(),
                critere.getCriterionFamily(),
                critere.getConfigurationVersion(),
                critere.getApplicability(),
                critere.getRequiredEvidenceTypes(),
                critere.getOptionalEvidenceTypes(),
                critere.getPositiveEvidenceExpectations(),
                critere.getNegativeEvidenceExpectations(),
                critere.getPermittedEvidenceNatures(),
                critere.getMinimumSupportingEvidenceCount(),
                critere.getScoringPolicy(),
                critere.getEliminatoryDecisionRule(),
                critere.getSourceAuthorityRules(),
                report.configurationComplete(),
                report.blockingMissingFields(),
                report.nonBlockingMissingFields(),
                report.configurationReasonCodes()
        );
    }
}
