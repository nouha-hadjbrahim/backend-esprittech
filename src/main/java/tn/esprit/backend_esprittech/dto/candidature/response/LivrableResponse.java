package tn.esprit.backend_esprittech.dto.candidature.response;

import tn.esprit.backend_esprittech.enums.TypeLivrable;

import java.time.Instant;

public record LivrableResponse(
        Long id,
        Long sujetId,
        TypeLivrable type,
        String nom,
        String lien,
        String description,
        boolean visible,
        Long deposantId,
        String deposantNom,
        Instant dateDepot
) {}
