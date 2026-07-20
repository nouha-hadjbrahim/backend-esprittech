package tn.esprit.backend_esprittech.entity.critereeval;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "criteres_evaluation")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class CritereEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String domaine;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false)
    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private ModeEvaluation modeEvaluation;

    @Column(length = 500)
    private String expectedLivrableTypes;

    private Integer minLivrableCount;

    @Column(length = 255)
    private String expectedKeyword;

    private Double noteMaxAuto;

    @Column(nullable = false)
    private Boolean ruleEnabled = false;

    @Column(length = 1000)
    private String ruleDescription;

    @Column(length = 100)
    private String criterionFamily;

    @Column(length = 40)
    private String configurationVersion;

    @Column(length = 120)
    private String applicability;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requiredEvidenceTypes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String optionalEvidenceTypes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String positiveEvidenceExpectations;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String negativeEvidenceExpectations;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String permittedEvidenceNatures;

    private Integer minimumSupportingEvidenceCount;

    @Column(length = 120)
    private String scoringPolicy;

    @Column(length = 120)
    private String eliminatoryDecisionRule;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String sourceAuthorityRules;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateMiseAJour;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDateTime.now();
        this.dateMiseAJour = LocalDateTime.now();

        if (this.actif == null) {
            this.actif = true;
        }
        if (this.ruleEnabled == null) {
            this.ruleEnabled = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dateMiseAJour = LocalDateTime.now();
    }


}
