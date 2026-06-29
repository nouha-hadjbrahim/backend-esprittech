package tn.esprit.backend_esprittech.dto.projetcatalogue.response;

import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Représentation canonique complète d'un projet du catalogue, renvoyée par les
 * endpoints de mutation (création, validation, refus).
 */
public record ProjetCatalogueResponse(
        Long id,
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
        Long equipeId,
        String equipeNom,
        Long chefValidateurId,
        String chefValidateurNom,
        String motifRefus,
        LocalDateTime dateCreation,
        LocalDateTime dateValidation,
        List<String> domaines,
        List<String> technologies,
        List<String> prerequis
) {
}
