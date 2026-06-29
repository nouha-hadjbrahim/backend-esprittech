package tn.esprit.backend_esprittech.dto.equipe;

import java.time.LocalDateTime;

public record EquipeDomaineResponse(
        Long id,
        String nom,
        LocalDateTime dateCreation
) {
}
