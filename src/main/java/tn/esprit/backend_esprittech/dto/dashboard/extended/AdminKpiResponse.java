package tn.esprit.backend_esprittech.dto.dashboard.extended;

public record AdminKpiResponse(
        long usersTotal,
        long encadrantsTotal,
        long equipesTotal,
        long affiliationsEnAttente,
        long sujetsTotal,
        long sujetsEnAttente,
        long sujetsValides,
        long sujetsInvalides,
        long candidaturesEnAttente,
        long candidaturesAcceptees,
        long candidaturesRefusees,
        long projetsCatalogue,
        long industrialisationsGo,
        long industrialisationsNoGo,
        double scoreMoyen
) {}
