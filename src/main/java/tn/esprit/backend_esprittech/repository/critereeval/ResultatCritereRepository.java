package tn.esprit.backend_esprittech.repository.critereeval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.critereeval.ResultatCritere;

import java.util.List;

public interface ResultatCritereRepository extends JpaRepository<ResultatCritere, Long> {

    List<ResultatCritere> findByEvaluationId(Long evaluationId);

    boolean existsByCritereId(Long critereId);

    boolean existsByNoteValue(Integer noteValue);
}
