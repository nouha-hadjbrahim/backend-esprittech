package tn.esprit.backend_esprittech.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdentifiant(String identifiant);

    List<User> findAllByIdIn(List<Long> ids);

    boolean existsByEmail(String email);

    boolean existsByIdentifiant(String identifiant);

    /** Recherche paginée par nom, prénom, email ou identifiant (insensible à la casse). */
    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.identifiant) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}
