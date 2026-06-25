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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCritere typeCritere;

    @Enumerated(EnumType.STRING)
    private ReponseEliminatoire reponseEliminatoire;

    private Double noteObtenue;

    private Double scorePondere;

    @Column(length = 2000)
    private String commentaire;
}
