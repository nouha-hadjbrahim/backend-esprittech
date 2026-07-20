package tn.esprit.backend_esprittech.entity.critereeval;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "criteres_eliminatoires")
public class CritereEliminatoire extends CritereEvaluation {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReponseEliminatoire reponseAttendue;
}