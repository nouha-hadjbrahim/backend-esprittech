package tn.esprit.backend_esprittech.dto.sujetprojet.response;

import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.time.LocalDateTime;
import java.util.List;

public record SujetProjetResponse(
        Long id,
        String titre,
        CategorieSujet categorie,
        String description,
        String objectifs,
        List<String> prerequis,
        List<String> domaines,
        List<String> technologies,
        Integer capaciteAccueil,
        StatutSujet statut,
        Double scoreFinal,
        Boolean eligibleIndustrialisation,
        Boolean catalogue,
        Long encadrantId,
        String encadrantNom,
        String encadrantEmail,
        String equipeNom,
        LocalDateTime dateCreation,
        LocalDateTime dateSoumission,
        LocalDateTime dateValidation,
        LocalDateTime dateDebutRealisation,
        LocalDateTime dateTerminaison,
        String motifInvalidation
) {
}
