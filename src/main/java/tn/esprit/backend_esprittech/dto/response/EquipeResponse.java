package tn.esprit.backend_esprittech.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Représentation d'une équipe de recherche exposée par l'API.
 */
public record EquipeResponse(
        Long id,
        String nom,
        String description,
        String domaine,
        UserResponse chef,
        List<UserResponse> members,
        int nbMembres,
        Instant createdAt,
        String statut
) {
}