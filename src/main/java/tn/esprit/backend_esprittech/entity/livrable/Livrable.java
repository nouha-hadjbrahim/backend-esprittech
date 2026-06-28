package tn.esprit.backend_esprittech.entity.livrable;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "livrables",
        indexes = {
                @Index(name = "idx_livrable_projet", columnList = "sujet_projet_id"),
                @Index(name = "idx_livrable_type", columnList = "type_livrable"),
                @Index(name = "idx_livrable_deposant", columnList = "deposant_id"),
                @Index(name = "idx_livrable_date_depot", columnList = "date_depot")
        }
)
public class Livrable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sujet_projet_id", nullable = false)
    private SujetProjet sujetProjet;

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
