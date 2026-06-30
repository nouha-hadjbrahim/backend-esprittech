package tn.esprit.backend_esprittech.dto.CritereEval;

import java.time.LocalDateTime;

public record NoteLevelResponse(
        Long id,
        Integer value,
        String label,
        String description,
        Boolean active,
        Integer order,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour
) {
}
