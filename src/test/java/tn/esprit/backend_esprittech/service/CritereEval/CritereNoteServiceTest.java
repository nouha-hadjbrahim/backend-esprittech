package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CritereNoteServiceTest {

    @Mock
    private CritereNoteRepository repository;

    @InjectMocks
    private CritereNoteService service;

    private CritereNote critere;

    @BeforeEach
    void setUp() {
        critere = new CritereNote();
        critere.setId(1L);
        critere.setLibelle("Critere note");
        critere.setDescription("Description");
        critere.setDomaine("Domaine");
        critere.setOrdre(1);
        critere.setActif(true);
        critere.setBareme(5.0);
        critere.setPoids(2.0);
        critere.setSeuil(3.0);
        critere.setDefaultNoteValue(3);
    }

    @Test
    void create_validRequest_shouldReturnSavedResponse() {
        when(repository.save(any(CritereNote.class))).thenAnswer(invocation -> {
            CritereNote saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CritereNoteRequest request = new CritereNoteRequest(
                "Critere note",
                "Description",
                "Domaine",
                1,
                null,
                2.0,
                null,
                4,
                true,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );

        var response = service.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Critere note", response.libelle());
        assertEquals(5.0, response.bareme());
        assertEquals(3.0, response.seuil());
        assertEquals(4, response.defaultNoteValue());
        verify(repository).save(any(CritereNote.class));
    }

    @Test
    void create_invalidBareme_shouldThrowBadRequestException() {
        CritereNoteRequest request = new CritereNoteRequest(
                "Critere note",
                "Description",
                "Domaine",
                1,
                0.0,
                2.0,
                1.0,
                true
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));

        assertEquals("Le bareme doit etre superieur a 0.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void create_invalidDefaultNote_shouldThrowBadRequestException() {
        CritereNoteRequest request = new CritereNoteRequest(
                "Critere note",
                "Description",
                "Domaine",
                1,
                null,
                2.0,
                null,
                6,
                true,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));

        assertEquals("La note par defaut doit etre comprise entre 1 et 5.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void update_existingCriterion_shouldReturnUpdatedResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CritereNoteRequest request = new CritereNoteRequest(
                "Autre critere",
                "Nouvelle description",
                "Domaine modifie",
                2,
                null,
                3.0,
                null,
                2,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );

        var response = service.update(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Autre critere", response.libelle());
        assertEquals(3.0, response.poids());
        assertEquals(2, response.defaultNoteValue());
        assertFalse(response.actif());
        verify(repository).findById(1L);
        verify(repository).save(any(CritereNote.class));
    }

    @Test
    void update_missingCriterion_shouldThrowResourceNotFoundException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        CritereNoteRequest request = new CritereNoteRequest(
                "Critere note",
                "Description",
                "Domaine",
                1,
                null,
                2.0,
                null,
                true
        );

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, request));

        assertEquals("Critere note introuvable avec id: 1", ex.getMessage());
        verify(repository).findById(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void deactivate_shouldSetActifFalse() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivate(1L);

        assertFalse(critere.getActif());
        verify(repository).save(critere);
    }

    @Test
    void activate_shouldSetActifTrue() {
        critere.setActif(false);
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.activate(1L);

        assertTrue(critere.getActif());
        verify(repository).save(critere);
    }

    @Test
    void reorder_invalidOrder_shouldThrowBadRequestException() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.reorder(List.of(new OrdreCritereRequest(1L, 0))));

        assertEquals("L'ordre doit etre superieur ou egal a 1.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void findAll_shouldReturnMappedResponses() {
        when(repository.findAll()).thenReturn(List.of(critere));

        var responses = service.findAll();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals(3, responses.get(0).defaultNoteValue());
        verify(repository).findAll();
    }

    @Test
    void findActive_shouldReturnMappedResponses() {
        when(repository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critere));

        var responses = service.findActive();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
        verify(repository).findByActifTrueOrderByOrdreAsc();
    }
}
