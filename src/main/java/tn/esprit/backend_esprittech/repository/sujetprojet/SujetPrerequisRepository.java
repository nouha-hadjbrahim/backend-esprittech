package tn.esprit.backend_esprittech.repository.sujetprojet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;

import java.util.List;
import java.util.Optional;

public interface SujetPrerequisRepository extends JpaRepository<SujetPrerequis, Long> {

    Page<SujetPrerequis> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    Page<SujetPrerequis> findAllByOrderByNomAsc(Pageable pageable);

    Optional<SujetPrerequis> findByNomIgnoreCase(String nom);

    List<SujetPrerequis> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);
}
