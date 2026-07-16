package tn.esprit.backend_esprittech.dto.accueil;

public record AccueilStatsResponse(
        long projetsActifs,
        long etudiants,
        long encadrants,
        long equipesRdi
) {}
