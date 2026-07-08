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
import tn.esprit.backend_esprittech.entity.historique.Historique;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.repository.historique.HistoriqueRepository;

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
}
