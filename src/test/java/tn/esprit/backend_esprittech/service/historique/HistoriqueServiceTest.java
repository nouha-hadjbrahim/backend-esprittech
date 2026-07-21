package tn.esprit.backend_esprittech.service.historique;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tn.esprit.backend_esprittech.dto.response.HistoriqueResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.exception.ForbiddenException;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.entity.historique.Historique;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.historique.HistoriqueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoriqueServiceTest {

    @Mock private HistoriqueRepository repository;
    @Mock private ObjectMapper objectMapper;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private ProjetCatalogueRepository projetCatalogueRepository;
    @Mock private UserService userService;

    @InjectMocks private HistoriqueService historiqueService;

    @Captor private ArgumentCaptor<Historique> historiqueCaptor;

    private final User actor = User.builder().id(1L).nom("Dupont").prenom("Jean").build();
    private final LocalDateTime now = LocalDateTime.of(2025, 6, 1, 10, 0);

    // ---- log ----

    @Test
    void log_withAllValues_savesHistorique() throws Exception {
        Map<String, Object> oldValues = Map.of("nom", "Old");
        Map<String, Object> newValues = Map.of("nom", "New");
        Map<String, Object> metadata = Map.of("motif", "Test");

        when(objectMapper.writeValueAsString(oldValues)).thenReturn("{\"nom\":\"Old\"}");
        when(objectMapper.writeValueAsString(newValues)).thenReturn("{\"nom\":\"New\"}");
        when(objectMapper.writeValueAsString(metadata)).thenReturn("{\"motif\":\"Test\"}");

        historiqueService.log(new HistoriqueService.LogRequest(ActionType.CREATE, EntityType.SUJET_PROJET, 42L,
                "Creation du projet", oldValues, newValues, metadata, actor, Role.ROLE_ADMIN));

        verify(repository).save(historiqueCaptor.capture());
        Historique h = historiqueCaptor.getValue();

        assertEquals(ActionType.CREATE, h.getAction());
        assertEquals(EntityType.SUJET_PROJET, h.getEntityType());
        assertEquals(42L, h.getEntityId());
        assertEquals("Creation du projet", h.getSummary());
        assertEquals(actor, h.getActor());
        assertEquals(Role.ROLE_ADMIN, h.getActorRole());
        assertEquals("{\"nom\":\"Old\"}", h.getOldValues());
        assertEquals("{\"nom\":\"New\"}", h.getNewValues());
        assertEquals("{\"motif\":\"Test\"}", h.getMetadata());
    }

    @Test
    void log_withNullMaps_serializesToNull() {
        historiqueService.log(new HistoriqueService.LogRequest(ActionType.DELETE, EntityType.CANDIDATURE, 7L,
                "Suppression", null, null, null, actor, Role.ROLE_ADMIN));

        verify(repository).save(historiqueCaptor.capture());
        Historique h = historiqueCaptor.getValue();

        assertNull(h.getOldValues());
        assertNull(h.getNewValues());
        assertNull(h.getMetadata());
    }

    @Test
    void log_withEmptyMaps_serializesToNull() {
        historiqueService.log(new HistoriqueService.LogRequest(ActionType.UPDATE, EntityType.LIVRABLE, 3L,
                "Mise a jour", Map.of(), Map.of(), Map.of(), actor, Role.ROLE_ADMIN));

        verify(repository).save(historiqueCaptor.capture());
        Historique h = historiqueCaptor.getValue();

        assertNull(h.getOldValues());
        assertNull(h.getNewValues());
        assertNull(h.getMetadata());
    }

    @Test
    void log_whenJsonFails_savesNull() throws Exception {
        Map<String, Object> oldValues = Map.of("nom", "Old");
        when(objectMapper.writeValueAsString(oldValues))
                .thenThrow(new JsonProcessingException("") {});

        historiqueService.log(new HistoriqueService.LogRequest(ActionType.CREATE, EntityType.SUJET_PROJET, 1L,
                "Test", oldValues, null, null, actor, Role.ROLE_ADMIN));

        verify(repository).save(historiqueCaptor.capture());
        assertNull(historiqueCaptor.getValue().getOldValues());
    }

    // ---- search ----

    @Test
    void search_delegatesToRepositoryAndMapsResponse() {
        Historique h = Historique.builder()
                .id(10L).actor(actor).actorRole(Role.ROLE_ADMIN)
                .action(ActionType.CREATE).entityType(EntityType.SUJET_PROJET).entityId(42L)
                .summary("Creation").createdAt(now)
                .build();
        Page<Historique> page = new PageImpl<>(List.of(h), PageRequest.of(0, 10), 1);
        when(repository.search(eq(EntityType.SUJET_PROJET), eq(42L), eq(ActionType.CREATE),
                eq(1L), any(), any(), any())).thenReturn(page);

        PagedResponse<HistoriqueResponse> result = historiqueService.search(
                EntityType.SUJET_PROJET, 42L, ActionType.CREATE, 1L,
                now.minusDays(1), now.plusDays(1), PageRequest.of(0, 10));

        assertEquals(1, result.totalElements());
        assertEquals(10L, result.content().get(0).id());
        assertEquals(1L, result.content().get(0).actorId());
    }

    // ---- findByEntity ----

    @Test
    void findByEntity_delegatesToRepositoryAndMapsResponse() {
        Historique h = Historique.builder()
                .id(20L).actor(actor).actorRole(Role.ROLE_ADMIN)
                .action(ActionType.UPDATE).entityType(EntityType.LIVRABLE).entityId(5L)
                .summary("Mise a jour").createdAt(now)
                .build();
        Page<Historique> page = new PageImpl<>(List.of(h), PageRequest.of(0, 5), 1);
        when(repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                EntityType.LIVRABLE, 5L, PageRequest.of(0, 5))).thenReturn(page);

        PagedResponse<HistoriqueResponse> result = historiqueService.findByEntity(
                EntityType.LIVRABLE, 5L, PageRequest.of(0, 5));

        assertEquals(1, result.totalElements());
        assertEquals(20L, result.content().get(0).id());
    }

    // ---- findByUser ----

    @Test
    void findByUser_delegatesToRepositoryAndMapsResponse() {
        Historique h = Historique.builder()
                .id(30L).actor(actor).actorRole(Role.ROLE_ADMIN)
                .action(ActionType.CREATE).entityType(EntityType.SUJET_PROJET).entityId(1L)
                .summary("Test").createdAt(now)
                .build();
        Page<Historique> page = new PageImpl<>(List.of(h), PageRequest.of(0, 10), 1);
        when(repository.findByActorIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .thenReturn(page);

        PagedResponse<HistoriqueResponse> result = historiqueService.findByUser(
                1L, PageRequest.of(0, 10));

        assertEquals(1, result.totalElements());
        assertEquals(30L, result.content().get(0).id());
        assertEquals("Dupont", result.content().get(0).actorNom());
    }

    // ---- findBySujet : filtres ----

    @Test
    void findBySujet_filterTout_shouldMergeSujetAndCandidatureEvents() {
        mockSujetAccess(Role.ROLE_ADMIN);
        when(repository.findAllBySujetProjetId(42L))
                .thenReturn(List.of(event(1L, ActionType.CREATE, now.minusHours(3))));
        when(candidatureRepository.findBySujetIdOrderByDateDepotAsc(42L)).thenReturn(List.of(candidature(5L)));
        when(repository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(eq(EntityType.CANDIDATURE), any()))
                .thenReturn(List.of(event(2L, ActionType.ACCEPT, now.minusHours(1))));

        var result = historiqueService.findBySujet(42L, "TOUT");

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).id(), "les evenements sont tries du plus recent au plus ancien");
    }

    @Test
    void findBySujet_filterSujet_shouldExcludeAcceptAndRefuse() {
        mockSujetAccess(Role.ROLE_ADMIN);
        when(repository.findAllBySujetProjetId(42L)).thenReturn(List.of(event(1L, ActionType.CREATE, now)));
        when(candidatureRepository.findBySujetIdOrderByDateDepotAsc(42L)).thenReturn(List.of(candidature(5L)));
        when(repository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(eq(EntityType.CANDIDATURE), any()))
                .thenReturn(List.of(
                        event(2L, ActionType.ACCEPT, now.minusHours(1)),
                        event(3L, ActionType.UPDATE, now.minusHours(2))));

        var result = historiqueService.findBySujet(42L, "SUJET");

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(r -> r.id() == 2L), "ACCEPT doit etre exclu du filtre SUJET");
    }

    @Test
    void findBySujet_filterCandidature_shouldKeepOnlyAcceptAndRefuse() {
        mockSujetAccess(Role.ROLE_ADMIN);
        when(candidatureRepository.findBySujetIdOrderByDateDepotAsc(42L)).thenReturn(List.of(candidature(5L)));
        when(repository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(eq(EntityType.CANDIDATURE), any()))
                .thenReturn(List.of(
                        event(2L, ActionType.ACCEPT, now.minusHours(1)),
                        event(3L, ActionType.UPDATE, now.minusHours(2)),
                        event(4L, ActionType.REFUSE, now.minusHours(3))));

        var result = historiqueService.findBySujet(42L, "CANDIDATURE");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.id() == 2L || r.id() == 4L));
        verify(repository, never()).findAllBySujetProjetId(any());
    }

    @Test
    void findBySujet_blankFilter_shouldDefaultToTout() {
        mockSujetAccess(Role.ROLE_ADMIN);
        when(repository.findAllBySujetProjetId(42L)).thenReturn(List.of(event(1L, ActionType.CREATE, now)));
        when(candidatureRepository.findBySujetIdOrderByDateDepotAsc(42L)).thenReturn(List.of());

        assertEquals(1, historiqueService.findBySujet(42L, "  ").size());
    }

    @Test
    void findBySujet_nullFilter_shouldDefaultToTout() {
        mockSujetAccess(Role.ROLE_ADMIN);
        when(repository.findAllBySujetProjetId(42L)).thenReturn(List.of(event(1L, ActionType.CREATE, now)));
        when(candidatureRepository.findBySujetIdOrderByDateDepotAsc(42L)).thenReturn(List.of());

        assertEquals(1, historiqueService.findBySujet(42L, null).size());
    }

    // ---- findBySujet : controle d'acces ----

    @Test
    void findBySujet_supervisingTeacher_shouldBeAllowed() {
        SujetProjet sujet = new SujetProjet();
        sujet.setId(42L);
        sujet.setEncadrantId(1L);
        when(sujetProjetRepository.findById(42L)).thenReturn(java.util.Optional.of(sujet));
        when(userService.getCurrentUser()).thenReturn(userWithRole(1L, Role.ROLE_ENSEIGNANT));
        when(repository.findAllBySujetProjetId(42L)).thenReturn(List.of());
        when(candidatureRepository.findBySujetIdOrderByDateDepotAsc(42L)).thenReturn(List.of());

        assertTrue(historiqueService.findBySujet(42L, "TOUT").isEmpty());
    }

    @Test
    void findBySujet_otherTeacher_shouldBeForbidden() {
        SujetProjet sujet = new SujetProjet();
        sujet.setId(42L);
        sujet.setEncadrantId(999L);
        when(sujetProjetRepository.findById(42L)).thenReturn(java.util.Optional.of(sujet));
        when(userService.getCurrentUser()).thenReturn(userWithRole(1L, Role.ROLE_ENSEIGNANT));

        assertThrows(ForbiddenException.class, () -> historiqueService.findBySujet(42L, "TOUT"));
    }

    @Test
    void findBySujet_unknownSujet_shouldReportNotFound() {
        when(sujetProjetRepository.findById(404L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> historiqueService.findBySujet(404L, "TOUT"));
    }

    // ---- findByProjetCatalogue ----

    @Test
    void findByProjetCatalogue_governanceRole_shouldReturnEvents() {
        ProjetCatalogue projet = new ProjetCatalogue();
        projet.setId(100L);
        when(projetCatalogueRepository.findById(100L)).thenReturn(java.util.Optional.of(projet));
        when(userService.getCurrentUser()).thenReturn(userWithRole(9L, Role.ROLE_CI));
        when(repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                eq(EntityType.PROJET_CATALOGUE), eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(event(7L, ActionType.CREATE, now))));

        var result = historiqueService.findByProjetCatalogue(100L);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).id());
    }

    @Test
    void findByProjetCatalogue_supervisingTeacher_shouldBeAllowed() {
        ProjetCatalogue projet = new ProjetCatalogue();
        projet.setId(100L);
        projet.setEncadrant(userWithRole(1L, Role.ROLE_ENSEIGNANT));
        when(projetCatalogueRepository.findById(100L)).thenReturn(java.util.Optional.of(projet));
        when(userService.getCurrentUser()).thenReturn(userWithRole(1L, Role.ROLE_ENSEIGNANT));
        when(repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                eq(EntityType.PROJET_CATALOGUE), eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        assertTrue(historiqueService.findByProjetCatalogue(100L).isEmpty());
    }

    @Test
    void findByProjetCatalogue_studentRole_shouldBeForbidden() {
        ProjetCatalogue projet = new ProjetCatalogue();
        projet.setId(100L);
        projet.setEncadrant(userWithRole(1L, Role.ROLE_ENSEIGNANT));
        when(projetCatalogueRepository.findById(100L)).thenReturn(java.util.Optional.of(projet));
        when(userService.getCurrentUser()).thenReturn(userWithRole(5L, Role.ROLE_ETUDIANT));

        assertThrows(ForbiddenException.class, () -> historiqueService.findByProjetCatalogue(100L));
    }

    @Test
    void findByProjetCatalogue_unknownProject_shouldReportNotFound() {
        when(projetCatalogueRepository.findById(404L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> historiqueService.findByProjetCatalogue(404L));
    }

    // ---- toJson ----

    @Test
    void log_withNullAndEmptyMaps_shouldStoreNullPayloads() {
        historiqueService.log(new HistoriqueService.LogRequest(ActionType.CREATE, EntityType.SUJET_PROJET, 42L,
                "Creation", null, Map.of(), null, actor, Role.ROLE_ADMIN));

        verify(repository).save(historiqueCaptor.capture());
        Historique h = historiqueCaptor.getValue();
        assertNull(h.getOldValues());
        assertNull(h.getNewValues());
        assertNull(h.getMetadata());
    }

    @Test
    void log_whenSerializationFails_shouldStoreNullPayload() throws JsonProcessingException {
        Map<String, Object> values = Map.of("k", "v");
        when(objectMapper.writeValueAsString(values)).thenThrow(new JsonProcessingException("boom") {});

        historiqueService.log(new HistoriqueService.LogRequest(ActionType.UPDATE, EntityType.SUJET_PROJET, 42L,
                "MAJ", values, null, null, actor, Role.ROLE_ADMIN));

        verify(repository).save(historiqueCaptor.capture());
        assertNull(historiqueCaptor.getValue().getOldValues());
    }

    private void mockSujetAccess(Role role) {
        SujetProjet sujet = new SujetProjet();
        sujet.setId(42L);
        sujet.setEncadrantId(1L);
        when(sujetProjetRepository.findById(42L)).thenReturn(java.util.Optional.of(sujet));
        when(userService.getCurrentUser()).thenReturn(userWithRole(9L, role));
    }

    private User userWithRole(Long id, Role role) {
        return User.builder().id(id).nom("Test").prenom("User").role(role).build();
    }

    private Historique event(Long id, ActionType action, LocalDateTime createdAt) {
        return Historique.builder()
                .id(id).actor(actor).actorRole(Role.ROLE_ADMIN)
                .action(action).entityType(EntityType.SUJET_PROJET).entityId(42L)
                .summary("Evenement " + id).createdAt(createdAt)
                .build();
    }

    private Candidature candidature(Long id) {
        Candidature candidature = new Candidature();
        candidature.setId(id);
        return candidature;
    }
}
