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

    @Lob
    @Column(name = "critere_description", columnDefinition = "TEXT")
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

    private Double mlScore;

    private Double mlMaxScore;

    private Double normalizedScore;

    private Double confidence;

    @Column(length = 80)
    private String criterionStatus;

    private Double evidenceQuality;

    private Double extractionQuality;

    private Double sourceCoverage;

    @Column(length = 80)
    private String eliminatoryState;

    @Lob
    @Column(name = "analysis_methods", columnDefinition = "TEXT")
    private String analysisMethods;

    @Lob
    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    private Integer noteValue;

    @Column(length = 255)
    private String noteLabel;

    @Column(length = 255)
    private String matchedRule;

    @Lob
    @Column(name = "metadata_used", columnDefinition = "TEXT")
    private String metadataUsed;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Lob
    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(length = 1000)
    private String evidenceLivrableIds;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(nullable = false)
    private Boolean ruleConfigured = false;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commentaire;
}
