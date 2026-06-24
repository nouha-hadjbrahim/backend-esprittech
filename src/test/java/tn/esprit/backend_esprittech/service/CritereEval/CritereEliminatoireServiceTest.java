package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereEliminatoireResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CritereEliminatoireServiceTest {

    @Mock
    private CritereEliminatoireRepository repository;

    @InjectMocks
    private CritereEliminatoireService service;

    private CritereEliminatoire critere;

    @BeforeEach
    void setUp() {
        critere = new CritereEliminatoire();
        critere.setId(1L);
        critere.setLibelle("Critère test");
        critere.setDescription("Description");
        critere.setDomaine("Domaine");
        critere.setOrdre(1);
        critere.setActif(true);
        critere.setReponseAttendue(ReponseEliminatoire.OK);
    }

    @Test
    void create_validRequest_shouldReturnSavedResponse() {
        when(repository.save(any(CritereEliminatoire.class))).thenAnswer(invocation -> {
            CritereEliminatoire saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CritereEliminatoireRequest request = new CritereEliminatoireRequest(
                "Critère test",
                "Description",
                "Domaine",
                1,
                ReponseEliminatoire.OK,
                true
        );

        CritereEliminatoireResponse response = service.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Critère test", response.libelle());
        assertTrue(response.actif());
        assertEquals(ReponseEliminatoire.OK, response.reponseAttendue());
        verify(repository).save(any(CritereEliminatoire.class));
    }

    @Test
    void create_invalidOrder_shouldThrowBadRequestException() {
        CritereEliminatoireRequest request = new CritereEliminatoireRequest(
                "Critère test",
                "Description",
                "Domaine",
                0,
                ReponseEliminatoire.OK,
                true
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));
        assertEquals("L'ordre doit être supérieur ou égal à 1.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void update_existingCriterion_shouldReturnUpdatedResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereEliminatoire.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CritereEliminatoireRequest request = new CritereEliminatoireRequest(
                "Autre critère",
                "Nouvelle description",
                "Domaine modifié",
                2,
                ReponseEliminatoire.NOT_OK,
                false
        );

        CritereEliminatoireResponse response = service.update(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Autre critère", response.libelle());
        assertEquals("Domaine modifié", response.domaine());
        assertFalse(response.actif());
        assertEquals(ReponseEliminatoire.NOT_OK, response.reponseAttendue());
        verify(repository).findById(1L);
        verify(repository).save(any(CritereEliminatoire.class));
    }

    @Test
    void update_missingCriterion_shouldThrowResourceNotFoundException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        CritereEliminatoireRequest request = new CritereEliminatoireRequest(
                "Critère test",
                "Description",
                "Domaine",
                1,
                ReponseEliminatoire.OK,
                true
        );

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, request));

        assertEquals("Critère éliminatoire introuvable avec id: 1", ex.getMessage());
        verify(repository).findById(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void deactivate_shouldSetActifFalse() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereEliminatoire.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivate(1L);

        assertFalse(critere.getActif());
        verify(repository).findById(1L);
        verify(repository).save(critere);
    }

    @Test
    void activate_shouldSetActifTrue() {
        critere.setActif(false);
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereEliminatoire.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.activate(1L);

        assertTrue(critere.getActif());
        verify(repository).findById(1L);
        verify(repository).save(critere);
    }

    @Test
    void reorder_invalidOrder_shouldThrowBadRequestException() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.reorder(List.of(new OrdreCritereRequest(1L, 0))));

        assertEquals("L'ordre doit être supérieur ou égal à 1.", ex.getMessage());
        verify(repository).findById(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void findAll_shouldReturnMappedResponses() {
        when(repository.findAll()).thenReturn(List.of(critere));

        var responses = service.findAll();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
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
