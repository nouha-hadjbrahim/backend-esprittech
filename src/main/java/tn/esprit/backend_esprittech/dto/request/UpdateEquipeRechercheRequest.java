package tn.esprit.backend_esprittech.dto.request;

import java.util.List;

public record UpdateEquipeRechercheRequest(
        String nom,
        String description,
        Long chefId,
        List<Long> memberIds
) {
}