package tn.esprit.backend_esprittech.repository.livrable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;

import java.util.List;

@Repository
public interface LivrableRepository extends JpaRepository<Livrable, Long> {

    List<Livrable> findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(Long sujetProjetId);

    List<Livrable> findByActifTrueOrderByDateDepotDesc();

    boolean existsBySujetProjetIdAndActifTrue(Long sujetProjetId);
}
