package tn.esprit.backend_esprittech.repository.sujetprojet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;

import java.util.List;
import java.util.Optional;

public interface SujetTechnologieRepository extends JpaRepository<SujetTechnologie, Long> {

    Page<SujetTechnologie> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    Page<SujetTechnologie> findAllByOrderByNomAsc(Pageable pageable);

    Optional<SujetTechnologie> findByNomIgnoreCase(String nom);

    List<SujetTechnologie> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);
}
