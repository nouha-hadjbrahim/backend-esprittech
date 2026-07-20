package tn.esprit.backend_esprittech.repository.sujetprojet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.util.List;

@Repository
public interface SujetProjetRepository extends JpaRepository<SujetProjet, Long> {

    long countByStatut(StatutSujet statut);

    long countByCategorie(CategorieSujet categorie);

    @Query(value = """
            SELECT domaine, COUNT(*) AS total
            FROM sujet_projets
            WHERE domaine IS NOT NULL AND TRIM(domaine) <> ''
            GROUP BY domaine
            ORDER BY total DESC
            LIMIT 20
            """, nativeQuery = true)
    List<Object[]> countTopDomaines();

    @Query("SELECT FUNCTION('YEAR', s.dateCreation) as y, FUNCTION('MONTH', s.dateCreation) as m, COUNT(s) FROM SujetProjet s GROUP BY y, m ORDER BY y, m")
    List<Object[]> countSujetsByMonth();

    List<SujetProjet> findByEncadrantIdOrderByDateCreationDesc(Long encadrantId);

    List<SujetProjet> findByEncadrantIdAndCategorieOrderByDateCreationDesc(Long encadrantId, CategorieSujet categorie);

    List<SujetProjet> findByEncadrantIdAndStatutOrderByDateCreationDesc(Long encadrantId, StatutSujet statut);

    List<SujetProjet> findByStatutOrderByDateCreationDesc(StatutSujet statut);

    List<SujetProjet> findByCatalogueTrueOrderByDateCreationDesc();

    List<SujetProjet> findByCatalogueTrueAndStatutInOrderByDateCreationDesc(List<StatutSujet> statuts);

    List<SujetProjet> findByStatutInOrderByDateCreationDesc(List<StatutSujet> statuts);

    List<SujetProjet> findByEncadrantIdInAndStatutInOrderByDateCreationDesc(
            List<Long> encadrantIds, List<StatutSujet> statuts);

    @Query("""
            SELECT s FROM SujetProjet s
            WHERE (:categorie IS NULL OR s.categorie = :categorie)
              AND (:statut IS NULL OR s.statut = :statut)
              AND (:search IS NULL OR :search = '' OR LOWER(s.titre) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.dateCreation DESC
            """)
    Page<SujetProjet> searchForAdmin(@Param("categorie") CategorieSujet categorie,
                                     @Param("statut") StatutSujet statut,
                                     @Param("search") String search,
                                     Pageable pageable);

    @Query("""
            SELECT s FROM SujetProjet s
            WHERE s.statut IN :statuts
              AND (:categorie IS NULL OR s.categorie = :categorie)
              AND (:search IS NULL OR :search = '' OR LOWER(s.titre) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.dateCreation DESC
            """)
    Page<SujetProjet> searchDemandesForAdmin(@Param("statuts") List<StatutSujet> statuts,
                                             @Param("categorie") CategorieSujet categorie,
                                             @Param("search") String search,
                                             Pageable pageable);

    @Query("""
            SELECT s FROM SujetProjet s
            WHERE s.catalogue = true
              AND s.statut IN :statuts
              AND (:categorie IS NULL OR s.categorie = :categorie)
              AND (:search IS NULL OR :search = '' OR LOWER(s.titre) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY s.dateCreation DESC
            """)
    Page<SujetProjet> searchDisponiblesForAdmin(@Param("statuts") List<StatutSujet> statuts,
                                                @Param("categorie") CategorieSujet categorie,
                                                @Param("search") String search,
                                                Pageable pageable);
}
