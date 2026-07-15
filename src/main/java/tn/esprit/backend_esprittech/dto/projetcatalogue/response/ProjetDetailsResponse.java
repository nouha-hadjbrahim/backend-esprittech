package tn.esprit.backend_esprittech.dto.projetcatalogue.response;

import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Représentation détaillée d'un projet pour les pages de détails (vue enseignant
 * « Mes projets » et vue publique « Catalogue »). Contient l'ensemble des informations
 * affichées : en-tête, cartes récapitulatives et listes de la barre latérale
 * (technologies, prérequis/classes cibles, mots-clés = domaines).
 */
public record ProjetDetailsResponse(
        Long id,
        /** Identifiant du sujet d'origine si ce projet a été publié automatiquement après terminaison, sinon null. */
        Long sujetId,
        TypeProjet typeProjet,
        String titre,
        String description,
        String objectifs,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutProjet statut,
        Integer score,
        Long encadrantId,
        String encadrantNom,
        String encadrantEmail,
        Long equipeId,
        String equipeNom,
        String chefEquipeNom,
        Long chefValidateurId,
        String chefValidateurNom,
        String motifRefus,
        LocalDateTime dateCreation,
        LocalDateTime dateValidation,
        List<String> domaines,
        List<String> technologies,
        List<String> prerequis,
        String coverImage
) {
}
