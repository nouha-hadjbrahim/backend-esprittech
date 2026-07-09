package tn.esprit.backend_esprittech.dto.projetcatalogue.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tn.esprit.backend_esprittech.enums.TypeProjet;

import java.time.LocalDate;
import java.util.List;

/**
 * Données soumises par l'enseignant pour déposer un projet complet au catalogue.
 *
 * <p>Aucun score, statut, encadrant ni équipe n'est accepté ici : ces valeurs sont
 * forcées côté serveur (statut = SoumisEnValidation, score = 0, encadrant = utilisateur
 * courant, équipe = celle de son affiliation acceptée).</p>
 */
public record CreateProjetRequest(

        @NotNull(message = "Le type de projet est obligatoire")
        TypeProjet typeProjet,

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
        String titre,

        @NotBlank(message = "La description est obligatoire")
        @Size(min = 10, message = "La description doit contenir au moins 10 caractères")
        String description,

        @NotBlank(message = "Les objectifs sont obligatoires")
        String objectifs,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        @NotEmpty(message = "Au moins un domaine doit être sélectionné")
        List<@NotNull @Positive(message = "Identifiant de domaine invalide") Long> domainesIds,

        @NotEmpty(message = "Au moins une technologie doit être sélectionnée")
        List<@NotNull @Positive(message = "Identifiant de technologie invalide") Long> technologiesIds,

        List<@NotNull @Positive(message = "Identifiant de prérequis invalide") Long> prerequisIds,

        /** Image de couverture encodée en base64 (data URI ou base64 brut), optionnelle. */
        String coverImageBase64,

        /** Type MIME de l'image de couverture (image/png ou image/jpeg). */
        String coverImageContentType
) {
}
