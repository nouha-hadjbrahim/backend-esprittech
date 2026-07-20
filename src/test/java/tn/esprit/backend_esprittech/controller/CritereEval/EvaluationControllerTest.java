package tn.esprit.backend_esprittech.controller.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationOverrideRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationValidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationControllerTest {

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private SujetProjetService sujetProjetService;

    @InjectMocks
    private EvaluationController controller;

    private EvaluationResponse evaluationResponse;
    private SujetProjetResponse sujetProjetResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        evaluationResponse = new EvaluationResponse(
                1L, 1L, 88.0, true, false, now, "Évaluation complétée", "ROLE_ADMIN Admin Root", "Recalcul manuel", List.of()
        );

        sujetProjetResponse = new SujetProjetResponse(
                1L, "Titre", CategorieSujet.PFE, "Desc", "Obj", List.of("Pre"),
                List.of("Dom"), List.of("Java"), 2, StatutSujet.REALISATION_TERMINEE,
                88.0, true, false, 1001L, "Encadrant 1", "encadrant@esprit.tn", null,
                now, now, null, null, null, null, false, 0, 0
        );
    }

    @Test
    void calculerEvaluation_shouldUseAutomaticEvaluationService() {
        when(evaluationService.calculateAutomaticEvaluation(1L, "Recalcul manuel")).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.calculerEvaluation(1L);

        assertEquals(evaluationResponse, result);
        verify(evaluationService).calculateAutomaticEvaluation(1L, "Recalcul manuel");
    }

    @Test
    void calculerScore_shouldUseAutomaticEvaluationService() {
        when(evaluationService.calculateAutomaticEvaluation(1L, "Recalcul manuel")).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.calculerScore(1L);

        assertEquals(evaluationResponse, result);
        verify(evaluationService).calculateAutomaticEvaluation(1L, "Recalcul manuel");
    }

    @Test
    void getLatestEvaluation_shouldReturnLatestResult() {
        when(evaluationService.getLatestEvaluationByProjet(1L)).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.getLatestEvaluation(1L);

        assertEquals(evaluationResponse, result);
        verify(evaluationService).getLatestEvaluationByProjet(1L);
    }

    @Test
    void getEvaluationHistory_shouldReturnHistory() {
        when(evaluationService.getEvaluationHistoryByProjet(1L)).thenReturn(List.of(evaluationResponse));

        List<EvaluationResponse> result = controller.getEvaluationHistory(1L);

        assertEquals(List.of(evaluationResponse), result);
        verify(evaluationService).getEvaluationHistoryByProjet(1L);
    }

    @Test
    void validerEvaluation_shouldDelegateRequest() {
        EvaluationValidationRequest request = new EvaluationValidationRequest("ok");
        when(evaluationService.validateLatestEvaluation(1L, request)).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.validerEvaluation(1L, request);

        assertEquals(evaluationResponse, result);
        verify(evaluationService).validateLatestEvaluation(1L, request);
    }

    @Test
    void rejeterEvaluation_shouldDelegateRequest() {
        EvaluationValidationRequest request = new EvaluationValidationRequest("ko");
        when(evaluationService.rejectLatestEvaluation(1L, request)).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.rejeterEvaluation(1L, request);

        assertEquals(evaluationResponse, result);
        verify(evaluationService).rejectLatestEvaluation(1L, request);
    }

    @Test
    void overrideEvaluation_shouldDelegateRequest() {
        EvaluationOverrideRequest request = new EvaluationOverrideRequest(90.0, "manual");
        when(evaluationService.overrideLatestEvaluation(1L, request)).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.overrideEvaluation(1L, request);

        assertEquals(evaluationResponse, result);
        verify(evaluationService).overrideLatestEvaluation(1L, request);
    }

    @Test
    void getEvaluableProjects_shouldReturnProjectList() {
        when(sujetProjetService.getProjetsEvaluables()).thenReturn(List.of(sujetProjetResponse));

        List<SujetProjetResponse> result = controller.getEvaluableProjects();

        assertEquals(1, result.size());
        verify(sujetProjetService).getProjetsEvaluables();
    }
}
