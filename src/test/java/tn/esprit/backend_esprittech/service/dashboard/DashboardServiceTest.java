package tn.esprit.backend_esprittech.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.dashboard.DashboardResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.StatutCandidature;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private ProjetCatalogueRepository projetCatalogueRepository;
    @Mock private CandidatureIndustrialisationRepository candidatureIndustrialisationRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        when(sujetProjetRepository.count()).thenReturn(10L);
        when(sujetProjetRepository.findByStatutOrderByDateCreationDesc(StatutSujet.INVALIDE))
                .thenReturn(List.of(new SujetProjet(), new SujetProjet()));
        when(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(any()))
                .thenReturn(List.of(new SujetProjet()));

        when(candidatureRepository.count()).thenReturn(20L);
        when(candidatureRepository.countByStatut(StatutCandidature.DEPOSEE)).thenReturn(5L);
        when(candidatureRepository.countByStatut(StatutCandidature.ACCEPTEE)).thenReturn(12L);
        when(candidatureRepository.countByStatut(StatutCandidature.REFUSEE)).thenReturn(3L);

        when(projetCatalogueRepository.findByStatutInOrderByDateCreationDesc(StatutProjet.STATUTS_CATALOGUE))
                .thenReturn(List.of());
        when(projetCatalogueRepository.countByStatut(StatutProjet.SOUMIS_EN_VALIDATION)).thenReturn(4L);

        when(candidatureIndustrialisationRepository.countByDecisionGoNoGoTrue()).thenReturn(6L);
        when(candidatureIndustrialisationRepository.countByDecisionGoNoGoFalse()).thenReturn(2L);
        when(candidatureIndustrialisationRepository.averageScoreEvaluationProjet()).thenReturn(78.456);
    }

    @Test
    void getDashboard_aggregatesAllKpisCorrectly() {
        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(10L, response.sujetsTotal());
        assertEquals(2L, response.sujetsInvalides());
        assertEquals(1L, response.sujetsEnAttente());
        assertEquals(7L, response.sujetsValides()); // 10 - 2 - 1

        assertEquals(20L, response.candidaturesTotal());
        assertEquals(5L, response.candidaturesEnAttente());
        assertEquals(12L, response.candidaturesAcceptees());
        assertEquals(3L, response.candidaturesRefusees());

        assertEquals(0L, response.projetsAuCatalogue());
        assertEquals(4L, response.projetsEnAttenteValidationCatalogue());

        assertEquals(6L, response.industrialisationsGo());
        assertEquals(2L, response.industrialisationsNoGo());
        assertEquals(78.46, response.scoreMoyen());
    }
}