package tn.esprit.backend_esprittech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Équipe de recherche créée par l'admin. Chaque équipe possède un chef
 * (un {@link User} de rôle CHEF_EQUIPE) qui valide les demandes d'affiliation.
 */
@Entity
@Table(name = "equipe_recherche", uniqueConstraints = {
        @UniqueConstraint(name = "uk_equipe_nom", columnNames = "nom")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipeRecherche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 1000)
    private String description;

    /** Chef de l'équipe : un enseignant promu au rôle CHEF_EQUIPE. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chef_id", foreignKey = @ForeignKey(name = "fk_equipe_chef"))
    private User chef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
