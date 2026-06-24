package tn.esprit.backend_esprittech.repository.sujetprojet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.util.List;

@Repository
public interface SujetProjetRepository extends JpaRepository<SujetProjet, Long> {

    List<SujetProjet> findByEncadrantIdOrderByDateCreationDesc(Long encadrantId);

    List<SujetProjet> findByEncadrantIdAndCategorieOrderByDateCreationDesc(Long encadrantId, CategorieSujet categorie);

    List<SujetProjet> findByEncadrantIdAndStatutOrderByDateCreationDesc(Long encadrantId, StatutSujet statut);

    List<SujetProjet> findByStatutOrderByDateCreationDesc(StatutSujet statut);
}
