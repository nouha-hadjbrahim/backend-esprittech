package tn.esprit.backend_esprittech.dto.sujetprojet.response;

import java.time.LocalDateTime;

public record ReferenceItemResponse(
        Long id,
        String nom,
        LocalDateTime dateCreation
) {
}
