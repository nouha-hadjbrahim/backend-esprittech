package tn.esprit.backend_esprittech.repository.projetcatalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;

import java.util.List;

@Repository
public interface ProjetCatalogueRepository extends JpaRepository<ProjetCatalogue, Long> {

    long countByStatut(StatutProjet statut);

    /** Tous les projets du catalogue applicatif, quel que soit leur statut (vue admin). */
    List<ProjetCatalogue> findAllByOrderByDateCreationDesc();

    /** Projets déposés par un encadrant (page « Mes projets »), les plus récents d'abord. */
    List<ProjetCatalogue> findByEncadrantIdOrderByDateCreationDesc(Long encadrantId);

    /** Projets d'une équipe dans un statut donné (page « Validation projets » du chef). */
    List<ProjetCatalogue> findByEquipeRechercheIdAndStatutOrderByDateCreationDesc(Long equipeId, StatutProjet statut);

    /** Projets dont le statut figure dans la liste fournie (statuts publics du catalogue). */
    List<ProjetCatalogue> findByStatutInOrderByDateCreationDesc(List<StatutProjet> statuts);

    /**
     * Catalogue public filtré. Seuls les projets dont le statut est dans {@code statuts}
     * (statuts publics) sont retournés. Tous les autres filtres sont optionnels : un
     * paramètre {@code null} (ou vide pour la recherche) désactive le critère associé.
     *
     * @param statuts   statuts visibles dans le catalogue (jamais vide)
     * @param type      type de projet, ou {@code null} pour tous
     * @param domaineId identifiant d'un domaine devant être associé au projet, ou {@code null}
     * @param annee     année de la date de début, ou {@code null} pour toutes
     * @param search    terme recherché dans le titre ou la description, ou {@code null}/vide
     */
    @Query("""
            SELECT DISTINCT p FROM ProjetCatalogue p
            WHERE p.statut IN :statuts
              AND (:type IS NULL OR p.typeProjet = :type)
              AND (:annee IS NULL OR YEAR(p.dateDebut) = :annee)
              AND (:domaineId IS NULL OR EXISTS (
                    SELECT 1 FROM p.domaines d WHERE d.id = :domaineId))
              AND (:search IS NULL OR :search = ''
                    OR LOWER(p.titre) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.dateCreation DESC
            """)
    List<ProjetCatalogue> rechercherCatalogue(@Param("statuts") List<StatutProjet> statuts,
                                              @Param("type") TypeProjet type,
                                              @Param("domaineId") Long domaineId,
                                              @Param("annee") Integer annee,
                                              @Param("search") String search);
}
