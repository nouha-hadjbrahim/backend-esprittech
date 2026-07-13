package tn.esprit.backend_esprittech.repository.historique;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.historique.Historique;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface HistoriqueRepository extends JpaRepository<Historique, Long> {

    @Query("""
            SELECT h FROM Historique h
            WHERE (:entityType IS NULL OR h.entityType = :entityType)
              AND (:entityId IS NULL OR h.entityId = :entityId)
              AND (:action IS NULL OR h.action = :action)
              AND (:actorId IS NULL OR h.actor.id = :actorId)
              AND (:dateFrom IS NULL OR h.createdAt >= :dateFrom)
              AND (:dateTo IS NULL OR h.createdAt <= :dateTo)
            ORDER BY h.createdAt DESC
            """)
    Page<Historique> search(@Param("entityType") EntityType entityType,
                             @Param("entityId") Long entityId,
                             @Param("action") ActionType action,
                             @Param("actorId") Long actorId,
                             @Param("dateFrom") LocalDateTime dateFrom,
                             @Param("dateTo") LocalDateTime dateTo,
                             Pageable pageable);

    Page<Historique> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(EntityType entityType, Long entityId, Pageable pageable);

    Page<Historique> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    @Query("""
            SELECT h FROM Historique h
            WHERE h.entityType = :entityType
              AND h.entityId IN :entityIds
            ORDER BY h.createdAt DESC
            """)
    List<Historique> findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(
            @Param("entityType") EntityType entityType,
            @Param("entityIds") Collection<Long> entityIds);

    @Query("""
            SELECT h FROM Historique h
            WHERE h.entityType = tn.esprit.backend_esprittech.enums.EntityType.SUJET_PROJET
              AND h.entityId = :sujetId
            ORDER BY h.createdAt DESC
            """)
    List<Historique> findAllBySujetProjetId(@Param("sujetId") Long sujetId);
}
