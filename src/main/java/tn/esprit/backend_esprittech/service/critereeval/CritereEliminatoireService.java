package tn.esprit.backend_esprittech.service.critereeval;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.critereeval.CritereEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CritereEliminatoireResponse;
import tn.esprit.backend_esprittech.dto.critereeval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.critereeval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.exception.critereeval.BadRequestException;
import tn.esprit.backend_esprittech.exception.critereeval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.critereeval.CritereEliminatoireRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CritereEliminatoireService {

    private final CritereEliminatoireRepository repository;
    private final CriterionConfigurationPolicy configurationPolicy;

    public CritereEliminatoireResponse create(CritereEliminatoireRequest request) {
        validateRequest(request);

        CritereEliminatoire critere = new CritereEliminatoire();
        applyRequest(critere, request);
        configurationPolicy.requireActiveComplete(critere, TypeCritere.ELIMINATOIRE);

        return toResponse(repository.save(critere));
    }

    public CritereEliminatoireResponse update(Long id, CritereEliminatoireRequest request) {
        validateRequest(request);

        CritereEliminatoire critere = getEntity(id);
        applyRequest(critere, request);
        configurationPolicy.requireActiveComplete(critere, TypeCritere.ELIMINATOIRE);

        return toResponse(repository.save(critere));
    }

    public void deactivate(Long id) {
        CritereEliminatoire critere = getEntity(id);
        critere.setActif(false);
        repository.save(critere);
    }

    public void activate(Long id) {
        CritereEliminatoire critere = getEntity(id);
        critere.setActif(true);
        configurationPolicy.requireActiveComplete(critere, TypeCritere.ELIMINATOIRE);
        repository.save(critere);
    }

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
        applyConfigurationFields(critere, request);

        if (request.actif() != null) {
            critere.setActif(request.actif());
        }
        critere.setRuleEnabled(Boolean.TRUE.equals(request.ruleEnabled()));
    }

    private void applyConfigurationFields(CritereEliminatoire critere, CritereEliminatoireRequest request) {
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

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
        CriterionConfigurationPolicy.ConfigurationReport report =
                configurationPolicy.inspect(critere, TypeCritere.ELIMINATOIRE);
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
