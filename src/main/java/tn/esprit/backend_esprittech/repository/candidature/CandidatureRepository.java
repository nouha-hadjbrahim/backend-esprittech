package tn.esprit.backend_esprittech.repository.candidature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.enums.StatutCandidature;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    long countByStatut(StatutCandidature statut);

    @Query(value = "SELECT YEAR(c.date_depot) as y, MONTH(c.date_depot) as m, COUNT(*) FROM candidatures c GROUP BY y, m ORDER BY y, m", nativeQuery = true)
    List<Object[]> countCandidaturesByMonth();

    /** Toutes les candidatures d'un sujet (vue encadrant) */
    List<Candidature> findBySujetIdOrderByDateDepotAsc(Long sujetId);

    /** Toutes les candidatures d'un étudiant (vue étudiant) */
    List<Candidature> findByEtudiantIdOrderByDateDepotDesc(Long etudiantId);

    /** Candidatures d'un sujet filtrées par statut */
    List<Candidature> findBySujetIdAndStatut(Long sujetId, StatutCandidature statut);

    /** Vérifier si un étudiant a déjà candidaté sur ce sujet (unicité) */
    Optional<Candidature> findBySujetIdAndEtudiantId(Long sujetId, Long etudiantId);

    /** Nombre de candidatures acceptées pour un sujet (pour vérif capacité) */
    long countBySujetIdAndStatut(Long sujetId, StatutCandidature statut);

    /** Autres candidatures en attente du même étudiant (pour archivage auto à l'acceptation) */
    List<Candidature> findByEtudiantIdAndStatut(Long etudiantId, StatutCandidature statut);
}
