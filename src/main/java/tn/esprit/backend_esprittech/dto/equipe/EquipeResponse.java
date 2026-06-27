package tn.esprit.backend_esprittech.dto.equipe;

import java.time.Instant;
import java.util.List;

import tn.esprit.backend_esprittech.dto.response.UserResponse;

/**
 * Représentation d'une équipe de recherche exposée par l'API.
 */
public record EquipeResponse(
        Long id,
        String nom,
        String description,
        Long domaineId,
        String domaine,
        UserResponse chef,
        List<UserResponse> members,
        int nbMembres,
        Instant createdAt,
        String statut
) {
}
