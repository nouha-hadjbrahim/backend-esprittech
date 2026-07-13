package tn.esprit.backend_esprittech.repository.equipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipeRechercheRepository extends JpaRepository<EquipeRecherche, Long> {

    boolean existsByNom(String nom);

    boolean existsByNomAndIdNot(String nom, Long id);

    boolean existsByChefId(Long chefId);

    boolean existsByChefIdAndIdNot(Long chefId, Long id);

    /** Équipe dont l'utilisateur donné est le chef (au plus une). */
    Optional<EquipeRecherche> findByChefId(Long chefId);

    @Query(value = "SELECT YEAR(e.created_at) as y, MONTH(e.created_at) as m, COUNT(*) FROM equipe_recherche e GROUP BY y, m ORDER BY y, m", nativeQuery = true)
    List<Object[]> countEquipesByMonth();
}

