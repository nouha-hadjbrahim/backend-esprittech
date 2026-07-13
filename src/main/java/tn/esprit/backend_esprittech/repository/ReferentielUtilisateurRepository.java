package tn.esprit.backend_esprittech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.ReferentielUtilisateur;

import java.util.Optional;

@Repository
public interface ReferentielUtilisateurRepository extends JpaRepository<ReferentielUtilisateur, Long> {

    Optional<ReferentielUtilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
}
