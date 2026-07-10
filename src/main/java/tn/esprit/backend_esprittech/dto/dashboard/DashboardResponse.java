package tn.esprit.backend_esprittech.dto.dashboard;

public record DashboardResponse(
        long sujetsTotal,
        long sujetsValides,
        long sujetsInvalides,
        long sujetsEnAttente,

        long candidaturesTotal,
        long candidaturesEnAttente,
        long candidaturesAcceptees,
        long candidaturesRefusees,

        long projetsAuCatalogue,
        long projetsEnAttenteValidationCatalogue,

        long industrialisationsGo,
        long industrialisationsNoGo,
        double scoreMoyen
) {}