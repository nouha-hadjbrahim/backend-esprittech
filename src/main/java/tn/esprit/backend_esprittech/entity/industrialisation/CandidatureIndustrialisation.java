package tn.esprit.backend_esprittech.entity.industrialisation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "candidatures_industrialisation",
        indexes = {
                @Index(name = "idx_candidature_indus_projet", columnList = "sujet_projet_id"),
                @Index(name = "idx_candidature_indus_statut", columnList = "statut")
        }
)
public class CandidatureIndustrialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sujet_projet_id", nullable = false)
    private SujetProjet sujetProjet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeIndustrialisation typeIndustrialisation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutIndustrialisation statut = StatutIndustrialisation.BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private User demandeur;

    @Column(nullable = false)
    private LocalDateTime dateDemande;

    private LocalDateTime dateSoumission;

    private LocalDateTime dateReceptionCI;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ci_decideur_id")
    private User ciDecideur;

    private LocalDateTime dateDecisionCI;

    private Boolean decisionGoNoGo;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private OrientationIndustrialisation orientation;

    @Column(length = 2000)
    private String motifDecision;

    private Double scoreEvaluationProjet;

    private Boolean hasEliminatoryWarnings = false;

    private Integer eliminatoryWarningsCount = 0;

    @Lob
    @Column(name = "eliminatory_warnings_details", columnDefinition = "TEXT")
    private String eliminatoryWarningsDetails;

    @Column(length = 2000)
    private String commentaire;

    @OneToMany(mappedBy = "candidatureIndustrialisation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReponseIndustrialisation> reponses = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateDemande == null) {
            dateDemande = LocalDateTime.now();
        }
        if (statut == null) {
            statut = StatutIndustrialisation.BROUILLON;
        }
        if (hasEliminatoryWarnings == null) {
            hasEliminatoryWarnings = false;
        }
        if (eliminatoryWarningsCount == null) {
            eliminatoryWarningsCount = 0;
        }
    }
}
