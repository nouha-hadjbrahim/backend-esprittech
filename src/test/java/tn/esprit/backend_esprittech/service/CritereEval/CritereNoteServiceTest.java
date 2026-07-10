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
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.ResultatCritereRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CritereNoteServiceTest {

    @Mock
    private CritereNoteRepository repository;

    @Mock
    private CritereNoteRuleRepository ruleRepository;

    @Mock
    private ResultatCritereRepository resultatCritereRepository;

    @Mock
    private NoteLevelService noteLevelService;

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

        lenient()
                .doNothing()
                .when(noteLevelService)
                .requireActiveValue(anyInt(), anyString());
    }

    @Test
    void create_validRequest_shouldReturnSavedResponse() {
        when(repository.save(any(CritereNote.class))).thenAnswer(invocation -> {
            CritereNote saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CritereNoteRequest request = createRequestWithDefaultNote(
                "Critere note",
                "Description",
                "Domaine",
                1,
                null,
                2.0,
                null,
                4,
                true
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

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.create(request)
        );

        assertEquals("Le bareme doit etre superieur a 0.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateLibelle_shouldThrowBusinessConflictException() {
        when(repository.findByLibelleIgnoreCase("Critere note"))
                .thenReturn(Optional.of(critere));

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

        BusinessConflictException ex = assertThrows(
                BusinessConflictException.class,
                () -> service.create(request)
        );

        assertEquals(
                "Un critere note existe deja avec le libelle: Critere note",
                ex.getMessage()
        );
        verify(repository, never()).save(any());
    }

    @Test
    void create_invalidDefaultNote_shouldThrowBadRequestException() {
        doThrow(new BadRequestException(
                "La note par defaut doit correspondre a un niveau de note actif configure."
        ))
                .when(noteLevelService)
                .requireActiveValue(6, "La note par defaut");

        CritereNoteRequest request = createRequestWithDefaultNote(
                "Critere note",
                "Description",
                "Domaine",
                1,
                null,
                2.0,
                null,
                6,
                true
        );

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.create(request)
        );

        assertEquals(
                "La note par defaut doit correspondre a un niveau de note actif configure.",
                ex.getMessage()
        );
        verify(repository, never()).save(any());
    }

    @Test
    void update_existingCriterion_shouldReturnUpdatedResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereNote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CritereNoteRequest request = createRequestWithDefaultNote(
                "Autre critere",
                "Nouvelle description",
                "Domaine modifie",
                2,
                null,
                3.0,
                null,
                2,
                false
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

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.update(1L, request)
        );

        assertEquals("Critere note introuvable avec id: 1", ex.getMessage());
        verify(repository).findById(1L);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_missingCriterion_shouldThrowResourceNotFoundException() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(404L)
        );

        assertEquals("Critere note introuvable avec id: 404", ex.getMessage());
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_usedCriterion_shouldThrowBusinessConflictException() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(resultatCritereRepository.existsByCritereId(1L)).thenReturn(true);

        BusinessConflictException ex = assertThrows(
                BusinessConflictException.class,
                () -> service.delete(1L)
        );

        assertEquals(
                "Ce critère est déjà utilisé dans des évaluations. "
                        + "Vous pouvez le désactiver au lieu de le supprimer.",
                ex.getMessage()
        );
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_unusedCriterion_shouldDeleteRulesThenCriterion() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(resultatCritereRepository.existsByCritereId(1L)).thenReturn(false);
        when(ruleRepository.findByCritereNoteIdOrderByPriorityAscIdAsc(1L))
                .thenReturn(List.of());

        service.delete(1L);

        verify(ruleRepository).deleteAll(List.of());
        verify(repository).delete(critere);
    }

    @Test
    void deactivate_shouldSetActifFalse() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereNote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivate(1L);

        assertFalse(critere.getActif());
        verify(repository).save(critere);
    }

    @Test
    void activate_shouldSetActifTrue() {
        critere.setActif(false);

        when(repository.findById(1L)).thenReturn(Optional.of(critere));
        when(repository.save(any(CritereNote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.activate(1L);

        assertTrue(critere.getActif());
        verify(repository).save(critere);
    }

    @Test
    void activate_missingCriterion_shouldThrowResourceNotFoundException() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.activate(404L)
        );
        verify(repository, never()).save(any());
    }

    @Test
    void deactivate_missingCriterion_shouldThrowResourceNotFoundException() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.deactivate(404L)
        );
        verify(repository, never()).save(any());
    }

    @Test
    void reorder_invalidOrder_shouldThrowBadRequestException() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.reorder(List.of(new OrdreCritereRequest(1L, 0)))
        );

        assertEquals(
                "L'ordre doit etre superieur ou egal a 1.",
                ex.getMessage()
        );
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
        when(repository.findByActifTrueOrderByOrdreAsc())
                .thenReturn(List.of(critere));

        var responses = service.findActive();

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());

        verify(repository).findByActifTrueOrderByOrdreAsc();
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsCritereFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(critere));

        var result = service.getOldValues(1L);

        assertEquals(critere.getLibelle(), result.get("libelle"));
        assertEquals(critere.getActif(), result.get("actif"));
        assertEquals(critere.getOrdre(), result.get("ordre"));
        assertEquals(critere.getDomaine(), result.get("domaine"));
        assertEquals(critere.getBareme(), result.get("bareme"));
        assertEquals(critere.getPoids(), result.get("poids"));
    }

    /**
     * Creates a request through the current 28-argument canonical constructor.
     *
     * The tests in this class only need the common criterion fields and
     * defaultNoteValue. All optional ML/evaluation configuration fields remain
     * unset, except the historical boolean field that was explicitly false in
     * the previous 16-argument calls.
     */
    private CritereNoteRequest createRequestWithDefaultNote(
            String libelle,
            String description,
            String domaine,
            Integer ordre,
            Double bareme,
            Double poids,
            Double seuil,
            Integer defaultNoteValue,
            Boolean actif
    ) {
        return new CritereNoteRequest(
                libelle,            // 1  String
                description,        // 2  String
                domaine,            // 3  String
                ordre,              // 4  Integer
                bareme,             // 5  Double
                poids,              // 6  Double
                seuil,              // 7  Double
                defaultNoteValue,   // 8  Integer
                actif,              // 9  Boolean
                null,               // 10 ModeEvaluation
                null,               // 11 String
                null,               // 12 Integer
                null,               // 13 String
                null,               // 14 Double
                false,              // 15 Boolean
                null,               // 16 String
                null,               // 17 String
                null,               // 18 String
                null,               // 19 String
                null,               // 20 String
                null,               // 21 String
                null,               // 22 String
                null,               // 23 String
                null,               // 24 String
                null,               // 25 Integer
                null,               // 26 String
                null,               // 27 String
                null                // 28 String
        );
    }
}
