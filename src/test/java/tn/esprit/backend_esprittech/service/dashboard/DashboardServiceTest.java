package tn.esprit.backend_esprittech.service.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.dashboard.DashboardResponse;
import tn.esprit.backend_esprittech.dto.dashboard.extended.ExtendedDashboardResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.historique.Historique;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutCandidature;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.equipe.AffiliationEnseignantRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.historique.HistoriqueRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private ProjetCatalogueRepository projetCatalogueRepository;
    @Mock private CandidatureIndustrialisationRepository candidatureIndustrialisationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EquipeRechercheRepository equipeRechercheRepository;
    @Mock private AffiliationEnseignantRepository affiliationEnseignantRepository;
    @Mock private HistoriqueRepository historiqueRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        lenient().when(sujetProjetRepository.count()).thenReturn(10L);
        lenient().when(sujetProjetRepository.findByStatutOrderByDateCreationDesc(StatutSujet.INVALIDE))
                .thenReturn(List.of(new SujetProjet(), new SujetProjet()));
        lenient().when(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(any()))
                .thenReturn(List.of(new SujetProjet()));
        lenient().when(sujetProjetRepository.countByStatut(StatutSujet.SOUMIS_EN_VALIDATION)).thenReturn(3L);
        lenient().when(sujetProjetRepository.countByStatut(StatutSujet.EN_ATTENTE)).thenReturn(2L);
        lenient().when(sujetProjetRepository.countByStatut(StatutSujet.INVALIDE)).thenReturn(2L);
        lenient().when(sujetProjetRepository.countSujetsByMonth()).thenReturn(List.of());

        lenient().when(candidatureRepository.count()).thenReturn(20L);
        lenient().when(candidatureRepository.countByStatut(StatutCandidature.DEPOSEE)).thenReturn(5L);
        lenient().when(candidatureRepository.countByStatut(StatutCandidature.ACCEPTEE)).thenReturn(12L);
        lenient().when(candidatureRepository.countByStatut(StatutCandidature.REFUSEE)).thenReturn(3L);
        lenient().when(candidatureRepository.countCandidaturesByMonth()).thenReturn(List.of());

        lenient().when(projetCatalogueRepository.findByStatutInOrderByDateCreationDesc(StatutProjet.STATUTS_CATALOGUE))
                .thenReturn(List.of());
        lenient().when(projetCatalogueRepository.countByStatut(StatutProjet.SOUMIS_EN_VALIDATION)).thenReturn(4L);
        lenient().when(projetCatalogueRepository.countByDomaine()).thenReturn(List.of());

        lenient().when(candidatureIndustrialisationRepository.countByDecisionGoNoGoTrue()).thenReturn(6L);
        lenient().when(candidatureIndustrialisationRepository.countByDecisionGoNoGoFalse()).thenReturn(2L);
        lenient().when(candidatureIndustrialisationRepository.averageScoreEvaluationProjet()).thenReturn(78.456);

        lenient().when(userRepository.count()).thenReturn(50L);
        lenient().when(userRepository.countByRole(Role.ROLE_ENSEIGNANT)).thenReturn(15L);
        lenient().when(userRepository.countByRole(Role.ROLE_CHEF_EQUIPE)).thenReturn(5L);
        lenient().when(userRepository.countByRole(Role.ROLE_ETUDIANT)).thenReturn(0L);
        lenient().when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(0L);
        lenient().when(userRepository.countByRole(Role.ROLE_CI)).thenReturn(0L);

        lenient().when(equipeRechercheRepository.count()).thenReturn(8L);
        lenient().when(equipeRechercheRepository.countEquipesByMonth()).thenReturn(List.of());

        lenient().when(affiliationEnseignantRepository.countByStatut(StatutAffiliation.EN_ATTENTE)).thenReturn(2L);

        lenient().when(historiqueRepository.findTop15ByOrderByCreatedAtDesc()).thenReturn(List.of());
        lenient().when(historiqueRepository.findByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    // ================================================================
    // getDashboard
    // ================================================================

    @Test
    void getDashboard_aggregatesAllKpisCorrectly() {
        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(10L, response.sujetsTotal());
        assertEquals(2L, response.sujetsInvalides());
        assertEquals(1L, response.sujetsEnAttente());
        assertEquals(7L, response.sujetsValides());

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

    @Test
    void getDashboard_nullAverageScore_returnsZero() {
        when(candidatureIndustrialisationRepository.averageScoreEvaluationProjet()).thenReturn(null);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0.0, response.scoreMoyen());
    }

    @Test
    void getDashboard_allEmptyLists_returnsCorrectCounts() {
        when(sujetProjetRepository.count()).thenReturn(0L);
        when(sujetProjetRepository.findByStatutOrderByDateCreationDesc(any())).thenReturn(List.of());
        when(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(any())).thenReturn(List.of());
        when(candidatureRepository.count()).thenReturn(0L);
        when(candidatureRepository.countByStatut(any())).thenReturn(0L);
        when(projetCatalogueRepository.findByStatutInOrderByDateCreationDesc(any())).thenReturn(List.of());
        when(projetCatalogueRepository.countByStatut(any())).thenReturn(0L);
        when(candidatureIndustrialisationRepository.countByDecisionGoNoGoTrue()).thenReturn(0L);
        when(candidatureIndustrialisationRepository.countByDecisionGoNoGoFalse()).thenReturn(0L);
        when(candidatureIndustrialisationRepository.averageScoreEvaluationProjet()).thenReturn(0.0);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0L, response.sujetsTotal());
        assertEquals(0L, response.candidaturesTotal());
        assertEquals(0L, response.industrialisationsGo());
        assertEquals(0.0, response.scoreMoyen());
    }

    // ================================================================
    // getExtendedDashboard
    // ================================================================

    @Test
    void getExtendedDashboard_aggregatesAllKpisAndCharts() {
        ExtendedDashboardResponse response = dashboardService.getExtendedDashboard(15);

        assertNotNull(response);
        assertNotNull(response.kpis());
        assertNotNull(response.trendsSujets());
        assertNotNull(response.trendsCandidatures());
        assertNotNull(response.trendsEquipes());
        assertNotNull(response.sujetsByStatut());
        assertNotNull(response.usersByRole());
        assertNotNull(response.catalogueByDomain());
        assertNotNull(response.recentActivity());

        assertEquals(50L, response.kpis().usersTotal());
        assertEquals(20L, response.kpis().encadrantsTotal());
        assertEquals(8L, response.kpis().equipesTotal());
        assertEquals(2L, response.kpis().affiliationsEnAttente());
        assertEquals(10L, response.kpis().sujetsTotal());
        assertEquals(5L, response.kpis().sujetsEnAttente());
        assertEquals(3L, response.kpis().sujetsValides());
        assertEquals(2L, response.kpis().sujetsInvalides());
        assertEquals(5L, response.kpis().candidaturesEnAttente());
        assertEquals(12L, response.kpis().candidaturesAcceptees());
        assertEquals(3L, response.kpis().candidaturesRefusees());
        assertEquals(0L, response.kpis().projetsCatalogue());
        assertEquals(6L, response.kpis().industrialisationsGo());
        assertEquals(2L, response.kpis().industrialisationsNoGo());
        assertEquals(78.46, response.kpis().scoreMoyen());

        assertEquals(3, response.sujetsByStatut().size());
        assertEquals(5, response.usersByRole().size());
        assertTrue(response.recentActivity().isEmpty());
    }

    @Test
    void getExtendedDashboard_emptyHistory_returnsEmptyActivity() {
        when(historiqueRepository.findByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        ExtendedDashboardResponse response = dashboardService.getExtendedDashboard(15);

        assertNotNull(response.recentActivity());
        assertTrue(response.recentActivity().isEmpty());
    }

    @Test
    void getExtendedDashboard_withHistoryItems_returnsFormattedActivity() {
        User actor = User.builder()
                .id(1L).prenom("Alice").nom("Dupont").email("alice@test.tn")
                .role(Role.ROLE_ADMIN).build();

        Historique h1 = Historique.builder()
                .id(1L).actor(actor).actorRole(Role.ROLE_ADMIN)
                .action(ActionType.VALIDATE).entityType(EntityType.SUJET_PROJET)
                .entityId(10L).summary("Sujet validé").createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        Historique h2 = Historique.builder()
                .id(2L).actor(actor).actorRole(Role.ROLE_ADMIN)
                .action(ActionType.CREATE).entityType(EntityType.CANDIDATURE)
                .entityId(20L).summary(null).createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(historiqueRepository.findByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(h1, h2)));

        ExtendedDashboardResponse response = dashboardService.getExtendedDashboard(15);

        assertEquals(2, response.recentActivity().size());
        assertEquals("Sujet validé", response.recentActivity().get(0).message());
        assertEquals("Alice Dupont", response.recentActivity().get(0).actor());
        assertEquals("validation", response.recentActivity().get(0).type());

        assertEquals("CREATE candidature", response.recentActivity().get(1).message());
        assertEquals("creation", response.recentActivity().get(1).type());
    }

    @Test
    void getExtendedDashboard_withTrendData_returnsCorrectTrends() {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        when(sujetProjetRepository.countSujetsByMonth())
                .thenReturn(List.<Object[]>of(new Object[]{currentYear, currentMonth, 5L}));
        when(candidatureRepository.countCandidaturesByMonth())
                .thenReturn(List.<Object[]>of(new Object[]{currentYear, currentMonth, 10L}));
        when(equipeRechercheRepository.countEquipesByMonth())
                .thenReturn(List.<Object[]>of(new Object[]{currentYear, currentMonth, 3L}));

        ExtendedDashboardResponse response = dashboardService.getExtendedDashboard(15);

        assertTrue(response.trendsSujets().stream().anyMatch(t -> t.value() > 0));
        assertTrue(response.trendsCandidatures().stream().anyMatch(t -> t.value() > 0));
        assertTrue(response.trendsEquipes().stream().anyMatch(t -> t.value() > 0));
    }
}
