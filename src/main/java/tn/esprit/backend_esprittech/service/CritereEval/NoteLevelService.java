package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.CritereEval.NoteLevelRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.NoteLevelResponse;
import tn.esprit.backend_esprittech.entity.CritereEval.NoteLevel;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.NoteLevelRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteLevelService {

    private static final Map<Integer, String> FALLBACK_LABELS = Map.of(
            1, "Très insatisfait",
            2, "Insatisfait",
            3, "Peu satisfait",
            4, "Satisfait",
            5, "Très satisfait"
    );

    private final NoteLevelRepository repository;

    @Transactional(readOnly = true)
    public List<NoteLevelResponse> findAll() {
        return repository.findAllByOrderByOrderIndexAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, String> activeLabelsByValue() {
        return repository.findByActiveTrueOrderByOrderIndexAsc().stream()
                .collect(Collectors.toMap(NoteLevel::getValue, NoteLevel::getLabel, (left, right) -> left));
    }

    @Transactional(readOnly = true)
    public String labelFor(Integer value) {
        if (value == null) {
            return null;
        }
        return repository.findByValue(value)
                .map(NoteLevel::getLabel)
                .orElse(FALLBACK_LABELS.getOrDefault(value, "Niveau " + value));
    }

    public NoteLevelResponse create(NoteLevelRequest request) {
        validate(request, null);
        NoteLevel level = new NoteLevel();
        apply(level, request);
        return toResponse(repository.save(level));
    }

    public NoteLevelResponse update(Long id, NoteLevelRequest request) {
        validate(request, id);
        NoteLevel level = getEntity(id);
        apply(level, request);
        return toResponse(repository.save(level));
    }

    public void activate(Long id) {
        NoteLevel level = getEntity(id);
        level.setActive(true);
        repository.save(level);
    }

    public void deactivate(Long id) {
        NoteLevel level = getEntity(id);
        level.setActive(false);
        repository.save(level);
    }

    private void validate(NoteLevelRequest request, Long id) {
        if (request.value() == null || request.value() < 1 || request.value() > 5) {
            throw new BadRequestException("La valeur du niveau doit etre comprise entre 1 et 5.");
        }
        if (request.label() == null || request.label().isBlank()) {
            throw new BadRequestException("Le libelle du niveau est obligatoire.");
        }
        if (request.order() == null || request.order() < 1) {
            throw new BadRequestException("L'ordre du niveau doit etre superieur ou egal a 1.");
        }
        repository.findByValue(request.value())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Un niveau existe deja pour la valeur " + request.value() + ".");
                });
    }

    private NoteLevel getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Niveau de note introuvable avec id: " + id));
    }

    private void apply(NoteLevel level, NoteLevelRequest request) {
        level.setValue(request.value());
        level.setLabel(request.label().trim());
        level.setDescription(request.description());
        level.setOrderIndex(request.order());
        if (request.active() != null) {
            level.setActive(request.active());
        }
    }

    private NoteLevelResponse toResponse(NoteLevel level) {
        return new NoteLevelResponse(
                level.getId(),
                level.getValue(),
                level.getLabel(),
                level.getDescription(),
                level.getActive(),
                level.getOrderIndex(),
                level.getDateCreation(),
                level.getDateMiseAJour()
        );
    }
}
