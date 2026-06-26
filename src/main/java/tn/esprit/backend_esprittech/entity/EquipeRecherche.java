package tn.esprit.backend_esprittech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.backend_esprittech.enums.StatutEquipe;

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
public class EquipeRecherche implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 200)
    private String domaine;

    /** Chef de l'équipe : un enseignant promu au rôle CHEF_EQUIPE. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chef_id", foreignKey = @ForeignKey(name = "fk_equipe_chef"))
    private User chef;

    /** Membres de l'équipe. */
    @Builder.Default
    @OneToMany(mappedBy = "equipeRecherche", fetch = FetchType.LAZY)
    private List<User> members = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutEquipe statut = StatutEquipe.ACTIF;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addMember(User user) {
        if (user == null) {
            return;
        }
        members.add(user);
        user.setEquipeRecherche(this);
    }

    public void removeMember(User user) {
        if (user == null) {
            return;
        }
        members.remove(user);
        if (user.getEquipeRecherche() == this) {
            user.setEquipeRecherche(null);
        }
    }
}