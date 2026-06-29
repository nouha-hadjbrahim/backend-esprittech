package tn.esprit.backend_esprittech.repository.industrialisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.industrialisation.HistoriqueIndustrialisation;

import java.util.List;

@Repository
public interface HistoriqueIndustrialisationRepository extends JpaRepository<HistoriqueIndustrialisation, Long> {

    List<HistoriqueIndustrialisation> findByCandidatureIndustrialisationIdOrderByDateTransitionAsc(Long candidatureId);
}
