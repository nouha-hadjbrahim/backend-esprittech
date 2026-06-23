package tn.esprit.backend_esprittech.entity.sujetprojet;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.time.LocalDateTime;

@Entity
@Table(name = "sujet_projets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SujetProjet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategorieSujet categorie;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objectifs;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prerequis;

    @Column(nullable = false, length = 150)
    private String domaine;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String technologies;

    @Column(nullable = false)
    private Integer capaciteAccueil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutSujet statut;

    private Double scoreFinal;

    @Builder.Default
    @Column(nullable = false)
    private Boolean eligibleIndustrialisation = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean catalogue = false;

    @Column(nullable = false)
    private Long encadrantId;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateSoumission;

    private LocalDateTime dateValidation;

    private LocalDateTime dateDebutRealisation;

    private LocalDateTime dateTerminaison;

    @Column(columnDefinition = "TEXT")
    private String motifInvalidation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (dateSoumission == null) {
            dateSoumission = LocalDateTime.now();
        }
    }
}
