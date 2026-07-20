package tn.esprit.backend_esprittech.entity.industrialisation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "reponses_industrialisation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reponse_indus_candidature_question",
                columnNames = {"candidature_id", "question_id"}
        )
)
public class ReponseIndustrialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private CandidatureIndustrialisation candidatureIndustrialisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionIndustrialisation question;

    @Lob
    @Column(name = "valeur_texte", columnDefinition = "TEXT")
    private String valeurTexte;

    private Boolean valeurBoolean;

    private Double valeurNumerique;

    @Column(length = 1000)
    private String valeurUrl;

    private String preuveObjectName;

    private String preuveOriginalFileName;

    private String preuveContentType;

    private Long preuveSize;

    @Enumerated(EnumType.STRING)
    private ReponseEliminatoire reponseEliminatoire;

    private Double noteObtenue;

    @Column(length = 2000)
    private String justificatif;

    @Column(nullable = false)
    private LocalDateTime dateReponse;

    @PrePersist
    void onCreate() {
        if (dateReponse == null) {
            dateReponse = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        dateReponse = LocalDateTime.now();
    }
}
