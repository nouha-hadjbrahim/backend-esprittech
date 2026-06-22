package tn.esprit.backend_esprittech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.ReferentielUtilisateur;

import java.util.Optional;

@Repository
public interface ReferentielUtilisateurRepository extends JpaRepository<ReferentielUtilisateur, Long> {

    /** Recherche une correspondance par email OU identifiant (source de vérité pour l'inscription). */
    Optional<ReferentielUtilisateur> findByEmailOrIdentifiant(String email, String identifiant);

    Optional<ReferentielUtilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdentifiant(String identifiant);
}
