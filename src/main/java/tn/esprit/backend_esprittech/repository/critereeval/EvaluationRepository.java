package tn.esprit.backend_esprittech.repository.critereeval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.critereeval.Evaluation;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findTopBySujetProjetIdOrderByDateCalculDescIdDesc(Long sujetProjetId);

    List<Evaluation> findBySujetProjetIdOrderByDateCalculDescIdDesc(Long sujetProjetId);

    Optional<Evaluation> findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(Long projetCatalogueId);

    List<Evaluation> findByProjetCatalogueIdOrderByDateCalculDescIdDesc(Long projetCatalogueId);

    Optional<Evaluation> findByIdAndProjetCatalogueId(Long id, Long projetCatalogueId);
}
