package tn.esprit.backend_esprittech.dto.equipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import tn.esprit.backend_esprittech.enums.equipe.StatutEquipe;

/**
 * Création d'une équipe de recherche par l'admin avec assignation d'un chef
 * et de membres optionnels.
 */
public record CreateEquipeRechercheRequest(

        @NotBlank(message = "Le nom de l'équipe est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String nom,

        @Size(max = 1000, message = "La description ne peut dépasser 1000 caractères")
        String description,

        @NotBlank(message = "Le domaine est obligatoire")
        @Size(max = 200, message = "Le domaine ne peut dépasser 200 caractères")
        String domaine,

        Long chefId,

        List<Long> memberIds,

        StatutEquipe statut
) {
}
