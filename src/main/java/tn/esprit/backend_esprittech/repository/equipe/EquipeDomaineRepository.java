package tn.esprit.backend_esprittech.repository.equipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.equipe.EquipeDomaine;

import java.util.List;
import java.util.Optional;

public interface EquipeDomaineRepository extends JpaRepository<EquipeDomaine, Long> {

    Page<EquipeDomaine> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    Page<EquipeDomaine> findAllByOrderByNomAsc(Pageable pageable);

    Optional<EquipeDomaine> findByNomIgnoreCase(String nom);

    List<EquipeDomaine> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);
}
