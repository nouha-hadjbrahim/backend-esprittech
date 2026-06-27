package tn.esprit.backend_esprittech.dto.equipe;

import java.util.List;

import tn.esprit.backend_esprittech.enums.equipe.StatutEquipe;

public record UpdateEquipeRechercheRequest(
        String nom,
        String description,
        Long domaineId,
        Long chefId,
        List<Long> memberIds,
        StatutEquipe statut
) {
}
