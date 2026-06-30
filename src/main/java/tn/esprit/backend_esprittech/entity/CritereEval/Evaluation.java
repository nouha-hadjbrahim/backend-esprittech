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

    @Column(nullable = false)
    private Boolean eligibleIndustrialisation;

    @Column(nullable = false)
    private Boolean bloqueParEliminatoire;

    private Boolean hasEliminatoryWarnings = false;

    private Integer eliminatoryWarningsCount = 0;

    @Column(length = 4000)
    private String eliminatoryWarningsDetails;

    @Column(nullable = false)
    private LocalDateTime dateCalcul;

    @Column(length = 2000)
    private String commentaire;

    @Column(length = 255)
    private String calculatedBy;

    @Column(length = 2000)
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
