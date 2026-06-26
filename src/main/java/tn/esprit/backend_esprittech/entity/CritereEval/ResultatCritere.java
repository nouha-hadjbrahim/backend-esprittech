package tn.esprit.backend_esprittech.entity.CritereEval;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "resultats_criteres")
public class ResultatCritere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Historical evaluation relation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    /**
     * We store critereId instead of a hard FK to preserve historical consistency
     * even if the criterion is later deactivated.
     */
    @Column(nullable = false)
    private Long critereId;

    @Column(nullable = false, length = 255)
    private String critereLibelle;

    @Column(length = 255)
    private String critereDomaine;

    @Column(length = 2000)
    private String critereDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCritere typeCritere;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private ModeEvaluation modeEvaluation;

    @Enumerated(EnumType.STRING)
    private ReponseEliminatoire reponseEliminatoire;

    private Double noteObtenue;

    private Double scorePondere;

    private Double bareme;

    private Double poids;

    @Column(length = 2000)
    private String evidenceSummary;

    @Column(length = 1000)
    private String evidenceLivrableIds;

    @Column(nullable = false)
    private Boolean ruleConfigured = false;

    @Column(length = 2000)
    private String commentaire;
}
