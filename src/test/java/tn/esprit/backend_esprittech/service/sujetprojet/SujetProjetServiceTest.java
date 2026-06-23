package tn.esprit.backend_esprittech.service.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.MockEncadrantResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SujetProjetServiceTest {

    @Mock
    private SujetProjetRepository sujetProjetRepository;

    @Mock
    private SujetProjetMapper sujetProjetMapper;

    @InjectMocks
    private SujetProjetService service;

    private SujetProjetRequest request;
    private SujetProjet entity;
    private SujetProjetResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        request = new SujetProjetRequest(
                "Sujet", CategorieSujet.PFE, "Desc", "Obj", "Pre", "Info",
                List.of("Java"), 2
        );

        entity = SujetProjet.builder()
                .id(1L)
                .titre("Sujet")
                .categorie(CategorieSujet.PFE)
                .description("Desc")
                .objectifs("Obj")
                .prerequis("Pre")
                .domaine("Info")
                .technologies("Java")
                .capaciteAccueil(2)
                .statut(StatutSujet.SOUMIS_EN_VALIDATION)
                .encadrantId(MockEncadrantContext.ENCADRANT_ID)
                .dateCreation(LocalDateTime.now())
                .build();

        response = new SujetProjetResponse(
                1L, "Sujet", CategorieSujet.PFE, "Desc", "Obj", "Pre", "Info",
                List.of("Java"), 2, StatutSujet.SOUMIS_EN_VALIDATION, null, false, false,
                MockEncadrantContext.ENCADRANT_ID, entity.getDateCreation(), entity.getDateCreation(),
                null, null, null, null
        );
    }

    @Test
    void creerSujet_savesAndReturnsResponse() {
        when(sujetProjetMapper.toEntity(request)).thenReturn(entity);
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(entity)).thenReturn(response);

        SujetProjetResponse result = service.creerSujet(request);

        assertEquals(response, result);
        verify(sujetProjetRepository).save(entity);
    }

    @Test
    void getMesSujets_noFilters_returnsAll() {
        when(sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(MockEncadrantContext.ENCADRANT_ID))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(entity)).thenReturn(response);

        List<SujetProjetResponse> results = service.getMesSujets(null, null);

        assertEquals(1, results.size());
        assertEquals(response, results.get(0));
    }

    @Test
    void getMesSujets_withCategorie_filtersByCategorie() {
        when(sujetProjetRepository.findByEncadrantIdAndCategorieOrderByDateCreationDesc(
                MockEncadrantContext.ENCADRANT_ID, CategorieSujet.PFE))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(entity)).thenReturn(response);

        List<SujetProjetResponse> results = service.getMesSujets(CategorieSujet.PFE, null);

        assertEquals(1, results.size());
    }

    @Test
    void getMesSujets_withStatut_filtersByStatut() {
        when(sujetProjetRepository.findByEncadrantIdAndStatutOrderByDateCreationDesc(
                MockEncadrantContext.ENCADRANT_ID, StatutSujet.SOUMIS_EN_VALIDATION))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(entity)).thenReturn(response);

        List<SujetProjetResponse> results = service.getMesSujets(null, StatutSujet.SOUMIS_EN_VALIDATION);

        assertEquals(1, results.size());
    }

    @Test
    void getMesSujets_withCategorieAndStatut_filtersInMemory() {
        SujetProjet other = SujetProjet.builder()
                .id(2L)
                .categorie(CategorieSujet.STAGE_INGENIEUR)
                .statut(StatutSujet.SOUMIS_EN_VALIDATION)
                .encadrantId(MockEncadrantContext.ENCADRANT_ID)
                .titre("Other")
                .description("D")
                .objectifs("O")
                .prerequis("P")
                .domaine("Dom")
                .technologies("Java")
                .capaciteAccueil(1)
                .build();

        when(sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(MockEncadrantContext.ENCADRANT_ID))
                .thenReturn(List.of(entity, other));
        when(sujetProjetMapper.toResponse(entity)).thenReturn(response);

        List<SujetProjetResponse> results = service.getMesSujets(
                CategorieSujet.PFE, StatutSujet.SOUMIS_EN_VALIDATION);

        assertEquals(1, results.size());
    }

    @Test
    void getSujetById_found_returnsResponse() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetMapper.toResponse(entity)).thenReturn(response);

        SujetProjetResponse result = service.getSujetById(1L);

        assertEquals(response, result);
    }

    @Test
    void getSujetById_notFound_throwsException() {
        when(sujetProjetRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getSujetById(99L));
        assertEquals("Sujet introuvable", ex.getMessage());
    }

    @Test
    void getSujetById_wrongEncadrant_throwsException() {
        entity.setEncadrantId(999L);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.getSujetById(1L));
    }

    @Test
    void modifierSujet_found_updatesAndReturns() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(entity)).thenReturn(response);

        SujetProjetResponse result = service.modifierSujet(1L, request);

        assertEquals(response, result);
        verify(sujetProjetMapper).updateEntity(entity, request);
    }

    @Test
    void modifierSujet_notFound_throwsException() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.modifierSujet(1L, request));
    }

    @Test
    void getTechnologiesDisponibles_returnsSortedList() {
        List<String> technologies = service.getTechnologiesDisponibles();

        assertFalse(technologies.isEmpty());
        assertEquals(technologies, technologies.stream().sorted().toList());
        assertTrue(technologies.contains("Java"));
        assertTrue(technologies.contains("Spring Boot"));
    }

    @Test
    void getMockEncadrant_returnsStaticData() {
        MockEncadrantResponse encadrant = service.getMockEncadrant();

        assertEquals(MockEncadrantContext.ENCADRANT_ID, encadrant.id());
        assertEquals(MockEncadrantContext.NOM_COMPLET, encadrant.nomComplet());
        assertEquals(MockEncadrantContext.ROLE, encadrant.role());
        assertEquals(MockEncadrantContext.EMAIL, encadrant.email());
        assertEquals(MockEncadrantContext.DEPARTEMENT, encadrant.departement());
    }
}
