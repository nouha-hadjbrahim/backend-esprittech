package tn.esprit.backend_esprittech.entity.CritereEval;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
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
     * Keep this relation with your existing SujetProjet entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sujet_projet_id", nullable = false)
    private SujetProjet sujetProjet;

    private Double scoreFinal;

    private Double mlScore;

    private Double finalValidatedScore;

    @Column(length = 40)
    private String mlStatus;

    @Column(length = 255)
    private String mlModelVersion;

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
