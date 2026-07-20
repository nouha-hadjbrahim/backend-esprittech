package tn.esprit.backend_esprittech.service.critereeval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.critereeval.NoteLevelRequest;
import tn.esprit.backend_esprittech.dto.critereeval.NoteLevelResponse;
import tn.esprit.backend_esprittech.entity.critereeval.NoteLevel;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.critereeval.BadRequestException;
import tn.esprit.backend_esprittech.exception.critereeval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.critereeval.NoteLevelRepository;
import tn.esprit.backend_esprittech.repository.critereeval.ResultatCritereRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteLevelService {

    public static final String NO_ACTIVE_NOTE_LEVEL_MESSAGE = "Aucun niveau de note actif n'est configure.";

    private static final Map<Integer, String> FALLBACK_LABELS = Map.of(
            1, "Très insatisfait",
            2, "Insatisfait",
            3, "Peu satisfait",
            4, "Satisfait",
            5, "Très satisfait"
    );

    private final NoteLevelRepository repository;
    private final CritereNoteRepository critereNoteRepository;
    private final CritereNoteRuleRepository critereNoteRuleRepository;
    private final ResultatCritereRepository resultatCritereRepository;

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
    public Set<Integer> activeValues() {
        return activeLevelsOrThrow().stream()
                .map(NoteLevel::getValue)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public int maxActiveNoteValue() {
        return repository.findTopByActiveTrueOrderByValueDesc()
                .map(NoteLevel::getValue)
                .orElseThrow(() -> new BusinessConflictException(NO_ACTIVE_NOTE_LEVEL_MESSAGE));
    }

    @Transactional(readOnly = true)
    public void requireActiveValue(Integer value, String fieldLabel) {
        if (value == null) {
            throw new BadRequestException(fieldLabel + " est obligatoire.");
        }
        if (value < 1) {
            throw new BadRequestException(fieldLabel + " doit etre superieure ou egale a 1.");
        }
        activeLevelsOrThrow();
        if (repository.findByValueAndActiveTrue(value).isEmpty()) {
            throw new BadRequestException(fieldLabel + " doit correspondre a un niveau de note actif configure.");
        }
    }

    @Transactional(readOnly = true)
    public int safeDefaultValue(Integer configured, int preferredDefault) {
        List<NoteLevel> activeLevels = activeLevelsOrThrow();
        Set<Integer> values = activeLevels.stream()
                .map(NoteLevel::getValue)
                .collect(Collectors.toSet());
        if (configured != null && values.contains(configured)) {
            return configured;
        }
        if (values.contains(preferredDefault)) {
            return preferredDefault;
        }
        return activeLevels.stream()
                .mapToInt(NoteLevel::getValue)
                .min()
                .orElseThrow(() -> new BusinessConflictException(NO_ACTIVE_NOTE_LEVEL_MESSAGE));
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

    public void delete(Long id) {
        NoteLevel level = getEntity(id);
        ensureNotUsed(level);
        repository.delete(level);
    }

    private void validate(NoteLevelRequest request, Long id) {
        if (request.value() == null || request.value() < 1) {
            throw new BadRequestException("La valeur du niveau doit etre superieure ou egale a 1.");
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
                    throw new BusinessConflictException("Un niveau existe deja pour la valeur " + request.value() + ".");
                });
        repository.findByOrderIndex(request.order())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Un niveau existe deja pour l'ordre " + request.order() + ".");
                });
    }

    private List<NoteLevel> activeLevelsOrThrow() {
        List<NoteLevel> activeLevels = repository.findByActiveTrueOrderByOrderIndexAsc();
        if (activeLevels.isEmpty()) {
            throw new BusinessConflictException(NO_ACTIVE_NOTE_LEVEL_MESSAGE);
        }
        return activeLevels;
    }

    private void ensureNotUsed(NoteLevel level) {
        Integer value = level.getValue();
        if (critereNoteRepository.existsByDefaultNoteValue(value)
                || critereNoteRuleRepository.existsByNoteValue(value)
                || resultatCritereRepository.existsByNoteValue(value)) {
            throw new BusinessConflictException(
                    "Ce niveau de note est deja utilise. Vous pouvez le desactiver au lieu de le supprimer."
            );
        }
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
