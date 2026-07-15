package tn.esprit.backend_esprittech.repository.CritereEval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findTopBySujetProjetIdOrderByDateCalculDesc(Long sujetProjetId);

    List<Evaluation> findBySujetProjetIdOrderByDateCalculDesc(Long sujetProjetId);

    Optional<Evaluation> findTopByProjetCatalogueIdOrderByDateCalculDesc(Long projetCatalogueId);

    List<Evaluation> findByProjetCatalogueIdOrderByDateCalculDesc(Long projetCatalogueId);
}
