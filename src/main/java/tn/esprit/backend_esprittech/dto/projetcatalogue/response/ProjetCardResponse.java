package tn.esprit.backend_esprittech.dto.projetcatalogue.response;

import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Représentation allégée d'un projet pour les listes en cartes
 * (Mes projets, Validation projets, Catalogue).
 */
public record ProjetCardResponse(
        Long id,
        TypeProjet typeProjet,
        String titre,
        String description,
        StatutProjet statut,
        Integer score,
        Long encadrantId,
        String encadrantNom,
        Long equipeId,
        String equipeNom,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDateTime dateCreation,
        List<String> domaines,
        List<String> technologies
) {
}
