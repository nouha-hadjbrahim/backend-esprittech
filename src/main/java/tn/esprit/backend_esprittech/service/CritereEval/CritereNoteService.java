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
                throw new BadRequestException("L'ordre doit être supérieur ou égal à 1.");
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
                .orElseThrow(() -> new ResourceNotFoundException("Critère noté introuvable avec id: " + id));
    }

    private void applyRequest(CritereNote critere, CritereNoteRequest request) {
        critere.setLibelle(request.libelle());
        critere.setDescription(request.description());
        critere.setDomaine(request.domaine());
        critere.setOrdre(request.ordre());
        critere.setBareme(request.bareme());
        critere.setPoids(request.poids());
        critere.setSeuil(request.seuil());

        if (request.actif() != null) {
            critere.setActif(request.actif());
        }
    }

    private void validateRequest(CritereNoteRequest request) {
        if (request.libelle() == null || request.libelle().isBlank()) {
            throw new BadRequestException("Le libellé est obligatoire.");
        }

        if (request.ordre() == null || request.ordre() < 1) {
            throw new BadRequestException("L'ordre doit être supérieur ou égal à 1.");
        }

        if (request.bareme() == null || request.bareme() <= 0) {
            throw new BadRequestException("Le barème doit être supérieur à 0.");
        }

        if (request.poids() == null || request.poids() <= 0) {
            throw new BadRequestException("Le poids doit être supérieur à 0.");
        }

        if (request.seuil() == null || request.seuil() < 0 || request.seuil() > request.bareme()) {
            throw new BadRequestException("Le seuil doit être compris entre 0 et le barème.");
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
                critere.getSeuil()
        );
    }
}