package tn.esprit.backend_esprittech.entity.historique;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.ActionType;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique", indexes = {
        @Index(name = "idx_historique_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_historique_actor", columnList = "actor_id"),
        @Index(name = "idx_historique_actor_date", columnList = "actor_id, created_at DESC"),
        @Index(name = "idx_historique_action", columnList = "action"),
        @Index(name = "idx_historique_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Historique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(name = "actor_role", nullable = false, length = 30)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActionType action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "old_values", columnDefinition = "JSON")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "JSON")
    private String newValues;

    @Column(columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
