package tn.esprit.backend_esprittech.repository.sujetprojet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;

import java.util.List;
import java.util.Optional;

public interface SujetDomaineRepository extends JpaRepository<SujetDomaine, Long> {

    Page<SujetDomaine> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    Page<SujetDomaine> findAllByOrderByNomAsc(Pageable pageable);

    Optional<SujetDomaine> findByNomIgnoreCase(String nom);

    List<SujetDomaine> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);
}
