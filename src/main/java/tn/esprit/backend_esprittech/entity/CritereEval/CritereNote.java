package tn.esprit.backend_esprittech.entity.CritereEval;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "criteres_notes")
public class CritereNote extends CritereEvaluation  {


    @Column(nullable = false)
    private Double bareme;

    @Column(nullable = false)
    private Double poids;

    @Column(nullable = false)
    private Double seuil;

}
