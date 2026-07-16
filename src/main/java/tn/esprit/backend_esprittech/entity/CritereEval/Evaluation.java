package tn.esprit.backend_esprittech.entity.CritereEval;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "evaluations")

public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sujet évalué lorsque l'évaluation porte sur le pipeline SujetProjet.
     * Nullable : exactement un des deux (sujetProjet, projetCatalogue) est renseigné.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sujet_projet_id")
    private SujetProjet sujetProjet;

    /**
     * Projet du catalogue applicatif évalué directement (dépôt manuel ou publié depuis un sujet).
     * Nullable : exactement un des deux (sujetProjet, projetCatalogue) est renseigné.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_catalogue_id")
    private ProjetCatalogue projetCatalogue;

    private Double scoreFinal;

    private Double mlScore;

    private Double finalValidatedScore;

    @Column(length = 40)
    private String mlStatus;

    @Column(name = "evaluation_trigger", length = 80)
    private String trigger;

    @Column(length = 120)
    private String correlationId;

    @Column(length = 80)
    private String engineType;

    @Column(length = 255)
    private String modelName;

    @Column(length = 255)
    private String mlModelVersion;

    @Column(length = 120)
    private String pipelineVersion;

    @Column(length = 80)
    private String criteriaVersion;

    @Column(length = 128)
    private String projectSnapshotHash;

    @Column(length = 128)
    private String deliverableSnapshotHash;

    @Column(length = 128)
    private String inputSnapshotHash;

    @Column(length = 80)
    private String processingStatus;

    @Column(length = 80)
    private String eligibilityStatus;

    private Double mlGlobalConfidence;

    @Lob
    @Column(name = "ml_warnings", columnDefinition = "TEXT")
    private String mlWarnings;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(length = 40)
    private String validationStatus = "PENDING";

    @Column(length = 255)
    private String validatedBy;

    private LocalDateTime validatedAt;

    @Lob
    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Lob
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Boolean eligibleIndustrialisation;

    @Column(nullable = false)
    private Boolean bloqueParEliminatoire;

    private Boolean hasEliminatoryWarnings = false;

    private Integer eliminatoryWarningsCount = 0;

    @Lob
    @Column(name = "eliminatory_warnings_details", columnDefinition = "TEXT")
    private String eliminatoryWarningsDetails;

    @Column(nullable = false)
    private LocalDateTime dateCalcul;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(length = 255)
    private String calculatedBy;

    @Lob
    @Column(name = "recalculation_reason", columnDefinition = "TEXT")
    private String recalculationReason;

    @OneToMany(
            mappedBy = "evaluation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ResultatCritere> resultats = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.dateCalcul = LocalDateTime.now();
    }

    public void addResultat(ResultatCritere resultat) {
        resultats.add(resultat);
        resultat.setEvaluation(this);
    }


}
