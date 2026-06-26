package tn.esprit.backend_esprittech.dto.livrable;

import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;

import java.time.LocalDateTime;

public record LivrableResponse(
        Long id,
        Long sujetProjetId,
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
        Boolean actif
) {
}
