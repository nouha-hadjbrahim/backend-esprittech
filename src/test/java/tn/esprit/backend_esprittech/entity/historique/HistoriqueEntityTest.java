package tn.esprit.backend_esprittech.entity.historique;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HistoriqueEntityTest {

    private Historique buildHistorique() {
        User actor = User.builder().id(1L).nom("Test").prenom("User").build();
        return Historique.builder()
                .actor(actor)
                .actorRole(Role.ROLE_ADMIN)
                .action(ActionType.CREATE)
                .entityType(EntityType.SUJET_PROJET)
                .entityId(42L)
                .summary("Creation sujet")
                .build();
    }

    @Test
    void onCreate_setsCreatedAtWhenNull() {
        Historique h = buildHistorique();
        assertNull(h.getCreatedAt());
        h.onCreate();
        assertNotNull(h.getCreatedAt());
    }

    @Test
    void onCreate_doesNotOverrideExistingCreatedAt() {
        Historique h = buildHistorique();
        LocalDateTime fixed = LocalDateTime.of(2025, 6, 1, 10, 0);
        h.setCreatedAt(fixed);
        h.onCreate();
        assertEquals(fixed, h.getCreatedAt());
    }

    @Test
    void builder_setsAllFields() {
        User actor = User.builder().id(1L).build();
        Historique h = Historique.builder()
                .id(10L)
                .actor(actor)
                .actorRole(Role.ROLE_ADMIN)
                .action(ActionType.UPDATE)
                .entityType(EntityType.LIVRABLE)
                .entityId(5L)
                .summary("Modification livrable")
                .oldValues("{\"nom\":\"Ancien\"}")
                .newValues("{\"nom\":\"Nouveau\"}")
                .metadata("{\"motif\":\"correction\"}")
                .build();
        assertEquals(10L, h.getId());
        assertEquals(actor, h.getActor());
        assertEquals(Role.ROLE_ADMIN, h.getActorRole());
        assertEquals(ActionType.UPDATE, h.getAction());
        assertEquals(EntityType.LIVRABLE, h.getEntityType());
        assertEquals(5L, h.getEntityId());
        assertEquals("Modification livrable", h.getSummary());
        assertEquals("{\"nom\":\"Ancien\"}", h.getOldValues());
        assertEquals("{\"nom\":\"Nouveau\"}", h.getNewValues());
        assertEquals("{\"motif\":\"correction\"}", h.getMetadata());
    }

    @Test
    void noArgsConstructor_works() {
        Historique h = new Historique();
        assertNull(h.getId());
        assertNull(h.getAction());
    }
}
