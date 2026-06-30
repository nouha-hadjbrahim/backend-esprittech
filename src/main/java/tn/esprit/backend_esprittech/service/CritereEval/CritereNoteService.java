package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CritereNoteService {

    private final CritereNoteRepository repository;

    public CritereNoteResponse create(CritereNoteRequest request) {
        validateRequest(request);
        CritereNote critere = new CritereNote();
        applyRequest(critere, request);
        return toResponse(repository.save(critere));
    }

    public CritereNoteResponse update(Long id, CritereNoteRequest request) {
        validateRequest(request);
        CritereNote critere = getEntity(id);
        applyRequest(critere, request);
        return toResponse(repository.save(critere));
    }

    public void deactivate(Long id) {
        CritereNote critere = getEntity(id);
        critere.setActif(false);
        repository.save(critere);
    }

    public void activate(Long id) {
        CritereNote critere = getEntity(id);
        critere.setActif(true);
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
        critere.setLibelle(request.libelle());
        critere.setDescription(request.description());
        critere.setDomaine(request.domaine());
        critere.setOrdre(request.ordre());
        critere.setBareme(request.bareme() == null ? 5.0 : request.bareme());
        critere.setPoids(request.poids());
        critere.setSeuil(request.seuil() == null ? 3.0 : request.seuil());
        critere.setDefaultNoteValue(request.defaultNoteValue() == null ? 3 : request.defaultNoteValue());
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

    private void validateRequest(CritereNoteRequest request) {
        if (request.libelle() == null || request.libelle().isBlank()) {
            throw new BadRequestException("Le libelle est obligatoire.");
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
        if (request.defaultNoteValue() != null && (request.defaultNoteValue() < 1 || request.defaultNoteValue() > 5)) {
            throw new BadRequestException("La note par defaut doit etre comprise entre 1 et 5.");
        }
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
