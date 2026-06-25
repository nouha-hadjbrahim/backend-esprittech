package tn.esprit.backend_esprittech.repository.candidature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.candidature.Livrable;
import tn.esprit.backend_esprittech.enums.TypeLivrable;

import java.util.List;

@Repository
public interface LivrableRepository extends JpaRepository<Livrable, Long> {

    List<Livrable> findBySujetIdOrderByDateDepotDesc(Long sujetId);

    List<Livrable> findBySujetIdAndType(Long sujetId, TypeLivrable type);
}
