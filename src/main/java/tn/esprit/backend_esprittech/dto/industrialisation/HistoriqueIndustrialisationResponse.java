package tn.esprit.backend_esprittech.dto.industrialisation;

import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;

import java.time.LocalDateTime;

public record HistoriqueIndustrialisationResponse(
        Long id,
        StatutIndustrialisation statutSource,
        StatutIndustrialisation statutCible,
        Long acteurId,
        String acteurNom,
        String roleActeur,
        LocalDateTime dateTransition,
        String motif,
        String commentaire
) {
}
