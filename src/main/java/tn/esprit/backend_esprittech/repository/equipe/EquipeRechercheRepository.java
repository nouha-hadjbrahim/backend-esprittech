package tn.esprit.backend_esprittech.repository.equipe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;

@Repository
public interface EquipeRechercheRepository extends JpaRepository<EquipeRecherche, Long> {

    boolean existsByNom(String nom);

    boolean existsByNomAndIdNot(String nom, Long id);

    boolean existsByChefId(Long chefId);

    boolean existsByChefIdAndIdNot(Long chefId, Long id);

    java.util.Optional<EquipeRecherche> findByChefId(Long chefId);
}

