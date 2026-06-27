package tn.esprit.backend_esprittech.dto.industrialisation;

public record ScoreBreakdownDto(
        String composant,
        String reference,
        String libelle,
        Double score,
        Double poids,
        String statut,
        String details,
        Boolean revueManuelleRequise
) {
}
