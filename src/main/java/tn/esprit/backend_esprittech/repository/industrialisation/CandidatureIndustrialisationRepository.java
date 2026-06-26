package tn.esprit.backend_esprittech.repository.industrialisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;

import java.util.Collection;
import java.util.List;

@Repository
public interface CandidatureIndustrialisationRepository extends JpaRepository<CandidatureIndustrialisation, Long> {

    List<CandidatureIndustrialisation> findByDemandeurIdOrderByDateDemandeDesc(Long demandeurId);

    List<CandidatureIndustrialisation> findByStatutInOrderByDateDemandeDesc(Collection<StatutIndustrialisation> statuts);

    List<CandidatureIndustrialisation> findAllByOrderByDateDemandeDesc();

    List<CandidatureIndustrialisation> findBySujetProjetIdOrderByDateDemandeDesc(Long sujetProjetId);
}
