package tn.esprit.backend_esprittech.entity.livrable;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;

import java.time.LocalDateTime;

/**
 * Livrable déposé sur un projet du catalogue applicatif ({@link ProjetCatalogue}).
 *
 * <p>Volontairement séparé de {@link Livrable} (qui est rattaché à {@code SujetProjet})
 * afin de ne pas interférer avec la fonctionnalité existante des sujets, conformément
 * au principe de tables dédiées adopté pour le catalogue.</p>
 */
@Getter
@Setter
@Entity
@Table(
        name = "livrables_catalogue",
        indexes = {
                @Index(name = "idx_livrable_cat_projet", columnList = "projet_catalogue_id"),
                @Index(name = "idx_livrable_cat_type", columnList = "type_livrable"),
                @Index(name = "idx_livrable_cat_deposant", columnList = "deposant_id"),
                @Index(name = "idx_livrable_cat_date_depot", columnList = "date_depot")
        }
)
public class LivrableCatalogue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projet_catalogue_id", nullable = false)
    private ProjetCatalogue projet;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_livrable", nullable = false, length = 30)
    private TypeLivrable typeLivrable;

    @Column(nullable = false)
    private String nom;

    @Column(length = 2000)
    private String description;

    private String originalFileName;

    @Column(unique = true)
    private String objectName;

    private String contentType;

    private Long size;

    @Column(length = 1000)
    private String lienExterne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposant_id", nullable = false)
    private User deposant;

    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot;

    @Column(nullable = false)
    private Boolean actif = true;

    @PrePersist
    void onCreate() {
        if (dateDepot == null) {
            dateDepot = LocalDateTime.now();
        }
        if (actif == null) {
            actif = true;
        }
    }
}
