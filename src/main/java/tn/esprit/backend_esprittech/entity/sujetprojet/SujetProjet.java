package tn.esprit.backend_esprittech.entity.sujetprojet;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.time.LocalDateTime;

@Entity
@Table(name = "sujet_projets")
@EntityListeners(SujetProjetEvaluationListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SujetProjet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères")
    @Column(nullable = false, length = 255)
    private String titre;

    @NotNull(message = "La catégorie est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategorieSujet categorie;

    @NotBlank(message = "La description est obligatoire")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Les objectifs sont obligatoires")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String objectifs;

    @NotBlank(message = "Les prérequis sont obligatoires")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prerequis;

    @NotBlank(message = "Au moins un domaine est obligatoire")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String domaine;

    @NotBlank(message = "Les technologies sont obligatoires")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String technologies;

    @NotNull(message = "La capacité d'accueil est obligatoire")
    @Min(value = 1, message = "La capacité d'accueil doit être au minimum 1")
    @Max(value = 50, message = "La capacité d'accueil ne doit pas dépasser 50")
    @Column(nullable = false)
    private Integer capaciteAccueil;

    @NotNull(message = "Le statut est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutSujet statut;

    private Double scoreFinalEvaluation;

    private Boolean bloqueParEliminatoire;

    @Builder.Default
    private Boolean hasEliminatoryWarnings = false;

    @Builder.Default
    private Integer eliminatoryWarningsCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean eligibleIndustrialisation = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean catalogue = false;

    @NotNull(message = "L'encadrant est obligatoire")
    @Positive(message = "L'identifiant encadrant doit être positif")
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
        if (hasEliminatoryWarnings == null) {
            hasEliminatoryWarnings = false;
        }
        if (eliminatoryWarningsCount == null) {
            eliminatoryWarningsCount = 0;
        }
    }
}
