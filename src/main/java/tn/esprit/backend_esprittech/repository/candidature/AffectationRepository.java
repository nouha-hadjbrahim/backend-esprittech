package tn.esprit.backend_esprittech.repository.candidature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
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

    @Query("SELECT COUNT(a) FROM Affectation a WHERE a.etudiant.id = :etudiantId AND a.statut = 'ACTIVE' AND EXISTS (SELECT 1 FROM SujetProjet s WHERE s.id = a.sujetId AND s.statut <> 'REALISATION_TERMINEE')")
    long countActiveByEtudiantIdOnOngoingProjects(Long etudiantId);
}
