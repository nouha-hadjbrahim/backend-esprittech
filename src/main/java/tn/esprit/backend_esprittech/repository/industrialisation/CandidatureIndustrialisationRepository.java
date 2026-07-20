package tn.esprit.backend_esprittech.repository.industrialisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;

import java.util.Collection;
import java.util.List;

@Repository
public interface CandidatureIndustrialisationRepository extends JpaRepository<CandidatureIndustrialisation, Long> {

    long countByDecisionGoNoGoTrue();

    long countByDecisionGoNoGoFalse();

    @Query("SELECT AVG(c.scoreEvaluationProjet) FROM CandidatureIndustrialisation c WHERE c.scoreEvaluationProjet IS NOT NULL")
    Double averageScoreEvaluationProjet();

    @Query(value = "SELECT YEAR(c.date_demande) as y, MONTH(c.date_demande) as m, COUNT(*) FROM candidatures_industrialisation c GROUP BY y, m ORDER BY y, m", nativeQuery = true)
    List<Object[]> countIndustrialisationsByMonth();

    List<CandidatureIndustrialisation> findByDemandeurIdOrderByDateDemandeDesc(Long demandeurId);

    List<CandidatureIndustrialisation> findByStatutInOrderByDateDemandeDesc(Collection<StatutIndustrialisation> statuts);

    List<CandidatureIndustrialisation> findAllByOrderByDateDemandeDesc();

    List<CandidatureIndustrialisation> findBySujetProjetIdOrderByDateDemandeDesc(Long sujetProjetId);

    List<CandidatureIndustrialisation> findByProjetCatalogueIdOrderByDateDemandeDesc(Long projetCatalogueId);
}
