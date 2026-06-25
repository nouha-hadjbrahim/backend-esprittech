package tn.esprit.backend_esprittech.entity.candidature;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.TypeLivrable;

import java.time.Instant;

@Entity
@Table(name = "livrables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Livrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sujet_id", nullable = false)
    private Long sujetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeLivrable type;

    @Column(nullable = false, length = 255)
    private String nom;

    /** URL ou chemin vers le fichier/dépôt Git */
    @Column(nullable = false, length = 1000)
    private String lien;

    @Column(length = 2000)
    private String description;

    /** true = visible à tous les acteurs du projet ; false = encadrant seulement */
    @Builder.Default
    @Column(nullable = false)
    private boolean visible = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposant_id", nullable = false)
    private User deposant;

    @Column(name = "date_depot", nullable = false, updatable = false)
    private Instant dateDepot;

    @PrePersist
    void onCreate() {
        if (dateDepot == null) {
            dateDepot = Instant.now();
        }
    }
}
