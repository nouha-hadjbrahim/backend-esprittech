package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.NoteLevelRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.NoteLevel;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.NoteLevelRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.ResultatCritereRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteLevelServiceTest {

    @Mock
    private NoteLevelRepository repository;

    @Mock
    private CritereNoteRepository critereNoteRepository;

    @Mock
    private CritereNoteRuleRepository critereNoteRuleRepository;

    @Mock
    private ResultatCritereRepository resultatCritereRepository;

    @InjectMocks
    private NoteLevelService service;

    @Test
    void create_valueSix_shouldSave() {
        NoteLevelRequest request = new NoteLevelRequest(6, "Excellent", "Niveau 6", true, 6);
        when(repository.findByValue(6)).thenReturn(Optional.empty());
        when(repository.findByOrderIndex(6)).thenReturn(Optional.empty());
        when(repository.save(any(NoteLevel.class))).thenAnswer(invocation -> saved(invocation.getArgument(0), 6L));

        var response = service.create(request);

        assertEquals(6, response.value());
        assertEquals(6, response.order());
        assertEquals("Excellent", response.label());
    }

    @Test
    void create_valueTen_shouldSave() {
        NoteLevelRequest request = new NoteLevelRequest(10, "Parfait", "Niveau 10", true, 10);
        when(repository.findByValue(10)).thenReturn(Optional.empty());
        when(repository.findByOrderIndex(10)).thenReturn(Optional.empty());
        when(repository.save(any(NoteLevel.class))).thenAnswer(invocation -> saved(invocation.getArgument(0), 10L));

        var response = service.create(request);

        assertEquals(10, response.value());
        assertEquals(10, response.order());
    }

    @Test
    void create_nonPositiveValue_shouldThrowBadRequest() {
        NoteLevelRequest request = new NoteLevelRequest(0, "Zero", null, true, 1);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));

        assertEquals("La valeur du niveau doit etre superieure ou egale a 1.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateValue_shouldThrowConflict() {
        NoteLevel existing = level(1L, 6, 6);
        when(repository.findByValue(6)).thenReturn(Optional.of(existing));

        NoteLevelRequest request = new NoteLevelRequest(6, "Excellent", null, true, 7);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class, () -> service.create(request));

        assertEquals("Un niveau existe deja pour la valeur 6.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateOrder_shouldThrowConflict() {
        when(repository.findByValue(7)).thenReturn(Optional.empty());
        when(repository.findByOrderIndex(6)).thenReturn(Optional.of(level(1L, 6, 6)));

        NoteLevelRequest request = new NoteLevelRequest(7, "Excellent", null, true, 6);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class, () -> service.create(request));

        assertEquals("Un niveau existe deja pour l'ordre 6.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void maxActiveNoteValue_shouldUseHighestActiveValue() {
        when(repository.findTopByActiveTrueOrderByValueDesc()).thenReturn(Optional.of(level(10L, 10, 10)));

        assertEquals(10, service.maxActiveNoteValue());
    }

    @Test
    void activeValues_withoutActiveLevels_shouldThrowConflict() {
        when(repository.findByActiveTrueOrderByOrderIndexAsc()).thenReturn(List.of());

        BusinessConflictException ex = assertThrows(BusinessConflictException.class, () -> service.activeValues());

        assertEquals(NoteLevelService.NO_ACTIVE_NOTE_LEVEL_MESSAGE, ex.getMessage());
    }

    @Test
    void delete_unusedLevel_shouldDelete() {
        NoteLevel level = level(6L, 6, 6);
        when(repository.findById(6L)).thenReturn(Optional.of(level));
        when(critereNoteRepository.existsByDefaultNoteValue(6)).thenReturn(false);
        when(critereNoteRuleRepository.existsByNoteValue(6)).thenReturn(false);
        when(resultatCritereRepository.existsByNoteValue(6)).thenReturn(false);

        service.delete(6L);

        verify(repository).delete(level);
    }

    @Test
    void delete_usedLevel_shouldThrowConflict() {
        when(repository.findById(6L)).thenReturn(Optional.of(level(6L, 6, 6)));
        when(critereNoteRepository.existsByDefaultNoteValue(6)).thenReturn(true);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class, () -> service.delete(6L));

        assertEquals("Ce niveau de note est deja utilise. Vous pouvez le desactiver au lieu de le supprimer.", ex.getMessage());
        verify(repository, never()).delete(any());
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsNoteLevelFields() {
        NoteLevel l = level(Long.valueOf(5), 5, 2);
        l.setLabel("Moyen");
        l.setActive(false);
        when(repository.findById(5L)).thenReturn(Optional.of(l));

        var result = service.getOldValues(5L);

        assertEquals(l.getValue(), result.get("value"));
        assertEquals(l.getLabel(), result.get("label"));
        assertEquals(l.getActive(), result.get("active"));
        assertEquals(l.getOrderIndex(), result.get("orderIndex"));
    }

    private NoteLevel saved(NoteLevel level, Long id) {
        level.setId(id);
        return level;
    }

    private NoteLevel level(Long id, Integer value, Integer order) {
        NoteLevel level = new NoteLevel();
        level.setId(id);
        level.setValue(value);
        level.setOrderIndex(order);
        level.setLabel("Niveau " + value);
        level.setActive(true);
        return level;
    }
}
