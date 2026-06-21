package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Création d'une équipe de recherche par l'admin avec assignation d'un chef.
 */
public record CreateEquipeRequest(

        @NotBlank(message = "Le nom de l'équipe est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String nom,

        @Size(max = 1000, message = "La description ne peut dépasser 1000 caractères")
        String description,

        @NotNull(message = "L'identifiant du chef est obligatoire")
        Long chefId
) {
}
