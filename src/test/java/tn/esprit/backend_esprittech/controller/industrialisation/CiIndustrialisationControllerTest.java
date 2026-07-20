package tn.esprit.backend_esprittech.controller.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionGoRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionNoGoRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionRecommandeeIndustrialisation;
import tn.esprit.backend_esprittech.dto.industrialisation.IndustrialisationScoreResponse;
import tn.esprit.backend_esprittech.entity.industrialisation.OrientationIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationScoreService;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CiIndustrialisationControllerTest {

    @Mock
    private IndustrialisationService industrialisationService;

    @Mock
    private IndustrialisationScoreService scoreService;

    @InjectMocks
    private CiIndustrialisationController controller;

    private CandidatureIndustrialisationResponse response;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        response = new CandidatureIndustrialisationResponse(
                11L,
                42L,
                "Plateforme IoT",
                StatutSujet.CANDIDAT_INDUSTRIALISATION_INTERNE,
                85.0,
                true,
                false,
                TypeIndustrialisation.INTERNE,
                StatutIndustrialisation.SOUMISE,
                7L,
                "Ali Mansour",
                now,
                now,
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Test
    void findRequests_withFilters_shouldDelegateToCiService() {
        when(industrialisationService.listForCi(StatutIndustrialisation.SOUMISE, TypeIndustrialisation.INTERNE))
                .thenReturn(List.of(response));

        List<CandidatureIndustrialisationResponse> result =
                controller.findRequests(StatutIndustrialisation.SOUMISE, TypeIndustrialisation.INTERNE);

        assertEquals(List.of(response), result);
        verify(industrialisationService).listForCi(StatutIndustrialisation.SOUMISE, TypeIndustrialisation.INTERNE);
    }

    @Test
    void detail_existingRequest_shouldReturnCiDetail() {
        when(industrialisationService.detailForCi(11L)).thenReturn(response);

        CandidatureIndustrialisationResponse result = controller.detail(11L);

        assertEquals(response, result);
        verify(industrialisationService).detailForCi(11L);
    }

    @Test
    void score_existingRequest_shouldReturnCalculatedScore() {
        IndustrialisationScoreResponse score = new IndustrialisationScoreResponse(
                11L,
                42L,
                87.0,
                DecisionRecommandeeIndustrialisation.GO,
                false,
                List.of(),
                90.0,
                80.0,
                90.0,
                List.of(),
                List.of(),
                "GO recommande"
        );
        when(scoreService.calculate(11L)).thenReturn(score);

        IndustrialisationScoreResponse result = controller.score(11L);

        assertEquals(score, result);
        verify(scoreService).calculate(11L);
    }

    @Test
    void go_validDecision_shouldDelegateToService() {
        DecisionGoRequest request = new DecisionGoRequest(OrientationIndustrialisation.DSI, "Industrialiser");
        CandidatureIndustrialisationResponse goResponse = new CandidatureIndustrialisationResponse(
                response.id(),
                response.projetId(),
                response.projetTitre(),
                StatutSujet.INDUSTRIALISE_DSI,
                response.scoreEvaluationProjet(),
                response.eligibleIndustrialisation(),
                response.bloqueParEliminatoire(),
                response.typeIndustrialisation(),
                StatutIndustrialisation.GO,
                response.demandeurId(),
                response.demandeurNom(),
                response.dateDemande(),
                response.dateSoumission(),
                response.dateReceptionCI(),
                99L,
                "CI User",
                LocalDateTime.now(),
                true,
                OrientationIndustrialisation.DSI,
                null,
                "Industrialiser",
                List.of(),
                List.of(),
                List.of()
        );
        when(industrialisationService.decideGo(11L, request)).thenReturn(goResponse);

        CandidatureIndustrialisationResponse result = controller.go(11L, request);

        assertEquals(StatutIndustrialisation.GO, result.statut());
        verify(industrialisationService).decideGo(11L, request);
    }

    @Test
    void noGo_validDecision_shouldDelegateToService() {
        DecisionNoGoRequest request = new DecisionNoGoRequest("Dossier incomplet");
        CandidatureIndustrialisationResponse noGoResponse = new CandidatureIndustrialisationResponse(
                response.id(),
                response.projetId(),
                response.projetTitre(),
                response.projetStatut(),
                response.scoreEvaluationProjet(),
                response.eligibleIndustrialisation(),
                response.bloqueParEliminatoire(),
                response.typeIndustrialisation(),
                StatutIndustrialisation.NO_GO,
                response.demandeurId(),
                response.demandeurNom(),
                response.dateDemande(),
                response.dateSoumission(),
                response.dateReceptionCI(),
                99L,
                "CI User",
                LocalDateTime.now(),
                false,
                null,
                "Dossier incomplet",
                null,
                List.of(),
                List.of(),
                List.of()
        );
        when(industrialisationService.decideNoGo(11L, request)).thenReturn(noGoResponse);

        CandidatureIndustrialisationResponse result = controller.noGo(11L, request);

        assertEquals(StatutIndustrialisation.NO_GO, result.statut());
        verify(industrialisationService).decideNoGo(11L, request);
    }

    @Test
    void score_convertedCandidature_shouldReturnPersistedProjectScoreWithout500() {
        IndustrialisationScoreResponse score = new IndustrialisationScoreResponse(
                18L,
                55L,
                84.0,
                DecisionRecommandeeIndustrialisation.GO,
                false,
                List.of(),
                0.0,
                0.0,
                84.0,
                List.of(),
                List.of(),
                "GO recommande"
        );
        when(scoreService.calculate(18L)).thenReturn(score);

        IndustrialisationScoreResponse result = controller.score(18L);

        assertEquals(84.0, result.scoreFinal());
        assertEquals(55L, result.projetId());
        assertEquals(18L, result.candidatureId());
        verify(scoreService).calculate(18L);
    }
}
