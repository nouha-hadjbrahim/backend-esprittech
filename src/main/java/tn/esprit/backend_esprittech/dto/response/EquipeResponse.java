package tn.esprit.backend_esprittech.dto.response;

import java.time.Instant;

/**
 * Représentation d'une équipe de recherche exposée par l'API.
 */
public record EquipeResponse(
        Long id,
        String nom,
        String description,
        UserResponse chef,
        int nbMembres,
        Instant createdAt
) {
}
