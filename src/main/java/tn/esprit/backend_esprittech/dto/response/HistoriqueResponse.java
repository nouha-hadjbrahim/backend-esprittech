package tn.esprit.backend_esprittech.dto.response;

import tn.esprit.backend_esprittech.entity.historique.Historique;
import tn.esprit.backend_esprittech.enums.ActionType;

import java.time.LocalDateTime;

public record HistoriqueResponse(
        Long id,
        Long actorId,
        String actorNom,
        String actorPrenom,
        String actorRole,
        ActionType action,
        String entityType,
        Long entityId,
        String summary,
        String oldValues,
        String newValues,
        String metadata,
        LocalDateTime createdAt
) {
    public static HistoriqueResponse from(Historique historique) {
        return new HistoriqueResponse(
                historique.getId(),
                historique.getActor().getId(),
                historique.getActor().getNom(),
                historique.getActor().getPrenom(),
                historique.getActorRole(),
                historique.getAction(),
                historique.getEntityType(),
                historique.getEntityId(),
                historique.getSummary(),
                historique.getOldValues(),
                historique.getNewValues(),
                historique.getMetadata(),
                historique.getCreatedAt()
        );
    }
}
