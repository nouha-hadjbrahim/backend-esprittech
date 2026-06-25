package tn.esprit.backend_esprittech.dto.sujetprojet.request;

import jakarta.validation.constraints.*;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;

import java.util.List;

public record SujetProjetRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères")
        String titre,

        @NotNull(message = "La catégorie est obligatoire")
        CategorieSujet categorie,

        @NotBlank(message = "La description est obligatoire")
        String description,

        @NotBlank(message = "Les objectifs sont obligatoires")
        String objectifs,

        @NotEmpty(message = "Au moins un prérequis doit être sélectionné")
        List<@NotBlank String> prerequis,

        @NotEmpty(message = "Au moins un domaine doit être sélectionné")
        List<@NotBlank @Size(max = 150) String> domaines,

        @NotEmpty(message = "Au moins une technologie doit être sélectionnée")
        List<@NotBlank String> technologies,

        @NotNull(message = "La capacité d'accueil est obligatoire")
        @Min(value = 1, message = "La capacité d'accueil doit être au minimum 1")
        @Max(value = 50, message = "La capacité d'accueil ne doit pas dépasser 50")
        Integer capaciteAccueil
) {
}
