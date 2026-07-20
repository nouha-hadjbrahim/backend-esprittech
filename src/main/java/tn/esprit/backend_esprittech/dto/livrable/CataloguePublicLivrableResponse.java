package tn.esprit.backend_esprittech.dto.livrable;

import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;

import java.time.LocalDateTime;

/**
 * Livrable exposé sur le détail public du catalogue.
 * Peut provenir du projet catalogue ou, à défaut, du sujet d'origine (lecture seule).
 */
public record CataloguePublicLivrableResponse(
        Long id,
        Long projetId,
        String projetTitre,
        TypeLivrable typeLivrable,
        String nom,
        String description,
        String originalFileName,
        String objectName,
        String contentType,
        Long size,
        String lienExterne,
        Long deposantId,
        String deposantNom,
        LocalDateTime dateDepot,
        Boolean actif,
        boolean fromSujet
) {
}
