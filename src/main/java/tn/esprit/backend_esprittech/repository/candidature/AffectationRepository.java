package tn.esprit.backend_esprittech.repository.candidature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;
import tn.esprit.backend_esprittech.enums.StatutAffectation;

import java.util.List;
import java.util.Optional;

@Repository
public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    List<Affectation> findBySujetId(Long sujetId);

    List<Affectation> findBySujetIdAndStatut(Long sujetId, StatutAffectation statut);

    Optional<Affectation> findBySujetIdAndEtudiantId(Long sujetId, Long etudiantId);

    long countBySujetIdAndStatut(Long sujetId, StatutAffectation statut);

    Optional<Affectation> findByCandidatureId(Long candidatureId);
}
