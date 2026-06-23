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

        @NotBlank(message = "Les prérequis sont obligatoires")
        String prerequis,

        @NotBlank(message = "Le domaine est obligatoire")
        @Size(max = 150, message = "Le domaine ne doit pas dépasser 150 caractères")
        String domaine,

        @NotEmpty(message = "Au moins une technologie doit être sélectionnée")
        List<@NotBlank String> technologies,

        @NotNull(message = "La capacité d'accueil est obligatoire")
        @Min(value = 1, message = "La capacité d'accueil doit être au minimum 1")
        @Max(value = 50, message = "La capacité d'accueil ne doit pas dépasser 50")
        Integer capaciteAccueil
) {
}
