package tn.esprit.backend_esprittech.entity.CritereEval;

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
    }

    @PreUpdate
    public void preUpdate() {
        this.dateMiseAJour = LocalDateTime.now();
    }


}
