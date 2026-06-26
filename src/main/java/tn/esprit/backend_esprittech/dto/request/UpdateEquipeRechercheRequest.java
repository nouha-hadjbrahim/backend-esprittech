package tn.esprit.backend_esprittech.dto.request;

import java.util.List;

import tn.esprit.backend_esprittech.enums.StatutEquipe;

public record UpdateEquipeRechercheRequest(
        String nom,
        String description,
        String domaine,
        Long chefId,
        List<Long> memberIds,
        StatutEquipe statut
) {
}