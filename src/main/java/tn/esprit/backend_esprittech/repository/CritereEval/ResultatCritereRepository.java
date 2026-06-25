package tn.esprit.backend_esprittech.repository.CritereEval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;

import java.util.List;

public interface ResultatCritereRepository extends JpaRepository<ResultatCritere, Long> {

    List<ResultatCritere> findByEvaluationId(Long evaluationId);
}