package tn.esprit.backend_esprittech.repository.equipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.equipe.AffiliationEnseignant;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;

import java.util.List;
import java.util.Optional;

@Repository
public interface AffiliationEnseignantRepository extends JpaRepository<AffiliationEnseignant, Long> {

    long countByStatut(StatutAffiliation statut);

    List<AffiliationEnseignant> findByEnseignantIdOrderByDateDemandeDesc(Long enseignantId);

    List<AffiliationEnseignant> findByEquipeIdOrderByDateDemandeDesc(Long equipeId);

    boolean existsByEnseignantIdAndEquipeIdAndStatut(Long enseignantId, Long equipeId, StatutAffiliation statut);

    List<AffiliationEnseignant> findByEquipeIdAndStatutOrderByDateDemandeDesc(Long equipeId, StatutAffiliation statut);

    Optional<AffiliationEnseignant> findByIdAndEquipeId(Long id, Long equipeId);

    List<AffiliationEnseignant> findByEnseignantIdAndStatut(Long enseignantId, StatutAffiliation statut);

    List<AffiliationEnseignant> findAllByOrderByDateDemandeDesc();
}
