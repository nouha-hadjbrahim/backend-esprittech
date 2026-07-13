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

import tn.esprit.backend_esprittech.enums.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findAllByIdIn(List<Long> ids);

    List<User> findByEquipeRechercheId(Long equipeId);

    boolean existsByEmail(String email);

    /** Recherche paginée par nom, prénom ou email (insensible à la casse). */
    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    /**
     * Recherche paginée des utilisateurs éligibles pour être chef ou membre
     * d'une équipe : filtre par rôle, exclut les utilisateurs déjà chef
     * d'une autre équipe ou déjà membres d'une autre équipe.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.role IN :roles
              AND (LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (u.equipeRecherche IS NULL OR u.equipeRecherche.id = :equipeId)
              AND NOT EXISTS (
                  SELECT 1 FROM EquipeRecherche e2
                  WHERE e2.chef.id = u.id AND e2.id != :equipeId)
            """)
    Page<User> searchEligible(@Param("q") String q,
                              @Param("equipeId") Long equipeId,
                              @Param("roles") List<Role> roles,
                              Pageable pageable);

    /**
     * Recherche paginée des utilisateurs éligibles pour être membres d'une équipe :
     * exclut les utilisateurs déjà membres d'une équipe ET tous les chefs.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.role IN :roles
              AND (LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
              AND u.equipeRecherche IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM EquipeRecherche e2
                  WHERE e2.chef.id = u.id)
            """)
    Page<User> searchEligibleMembers(@Param("q") String q,
                                     @Param("roles") List<Role> roles,
                                     Pageable pageable);

    /**
     * Enseignants et chefs d'équipe affiliés à une équipe de recherche
     * (membre ou chef d'une équipe).
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.role IN :roles
              AND (
                u.equipeRecherche IS NOT NULL
                OR EXISTS (SELECT 1 FROM EquipeRecherche e WHERE e.chef.id = u.id)
              )
              AND (
                :q = ''
                OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<User> searchAffiliatedEncadrants(@Param("q") String q,
                                          @Param("roles") List<Role> roles,
                                          Pageable pageable);
}
