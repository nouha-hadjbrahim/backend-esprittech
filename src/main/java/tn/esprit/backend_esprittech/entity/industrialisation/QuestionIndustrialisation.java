package tn.esprit.backend_esprittech.entity.industrialisation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "questions_industrialisation",
        indexes = {
                @Index(name = "idx_question_indus_actif", columnList = "actif"),
                @Index(name = "idx_question_indus_ordre", columnList = "ordre")
        }
)
public class QuestionIndustrialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeReponseIndustrialisation typeReponse;

    @Column(nullable = false)
    private Boolean obligatoire = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeCritere typeCritere;

    private Double poids;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false)
    private Boolean actif = true;

    private Boolean conditionEliminatoire;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateMiseAJour;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        dateCreation = now;
        dateMiseAJour = now;
        if (actif == null) {
            actif = true;
        }
        if (obligatoire == null) {
            obligatoire = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        dateMiseAJour = LocalDateTime.now();
    }
}
