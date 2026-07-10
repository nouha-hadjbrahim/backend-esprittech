package tn.esprit.backend_esprittech.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.backend_esprittech.dto.dashboard.DashboardResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.StatutCandidature;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<StatutSujet> SUJET_STATUTS_EN_ATTENTE =
            List.of(StatutSujet.SOUMIS_EN_VALIDATION, StatutSujet.EN_ATTENTE);

    private final SujetProjetRepository sujetProjetRepository;
    private final CandidatureRepository candidatureRepository;
    private final ProjetCatalogueRepository projetCatalogueRepository;
    private final CandidatureIndustrialisationRepository candidatureIndustrialisationRepository;

    public DashboardResponse getDashboard() {
        long sujetsTotal = sujetProjetRepository.count();
        long sujetsInvalides = sujetProjetRepository
                .findByStatutOrderByDateCreationDesc(StatutSujet.INVALIDE).size();
        long sujetsEnAttente = sujetProjetRepository
                .findByStatutInOrderByDateCreationDesc(SUJET_STATUTS_EN_ATTENTE).size();
        long sujetsValides = sujetsTotal - sujetsInvalides - sujetsEnAttente;

        long candidaturesTotal = candidatureRepository.count();
        long candidaturesEnAttente = candidatureRepository.countByStatut(StatutCandidature.DEPOSEE);
        long candidaturesAcceptees = candidatureRepository.countByStatut(StatutCandidature.ACCEPTEE);
        long candidaturesRefusees = candidatureRepository.countByStatut(StatutCandidature.REFUSEE);

        long projetsAuCatalogue = projetCatalogueRepository
                .findByStatutInOrderByDateCreationDesc(StatutProjet.STATUTS_CATALOGUE).size();
        long projetsEnAttenteValidationCatalogue = projetCatalogueRepository
                .countByStatut(StatutProjet.SOUMIS_EN_VALIDATION);

        long industrialisationsGo = candidatureIndustrialisationRepository.countByDecisionGoNoGoTrue();
        long industrialisationsNoGo = candidatureIndustrialisationRepository.countByDecisionGoNoGoFalse();

        Double avg = candidatureIndustrialisationRepository.averageScoreEvaluationProjet();
        double scoreMoyen = avg == null ? 0.0 : Math.round(avg * 100.0) / 100.0;

        return new DashboardResponse(
                sujetsTotal, sujetsValides, sujetsInvalides, sujetsEnAttente,
                candidaturesTotal, candidaturesEnAttente, candidaturesAcceptees, candidaturesRefusees,
                projetsAuCatalogue, projetsEnAttenteValidationCatalogue,
                industrialisationsGo, industrialisationsNoGo, scoreMoyen
        );
    }
}