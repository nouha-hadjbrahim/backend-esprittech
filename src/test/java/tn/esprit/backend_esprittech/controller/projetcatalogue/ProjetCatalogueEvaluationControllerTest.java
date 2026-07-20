package tn.esprit.backend_esprittech.controller.projetcatalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationResponse;
import tn.esprit.backend_esprittech.service.critereeval.EvaluationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjetCatalogueEvaluationControllerTest {

    @Mock
    private EvaluationService evaluationService;

    @InjectMocks
    private ProjetCatalogueEvaluationController controller;

    private EvaluationResponse response;

    @BeforeEach
    void setUp() {
        response = new EvaluationResponse(
                10L,
                null,
                7L,
                "PROJET_CATALOGUE",
                88.0,
                true,
                false,
                false,
                0,
                List.of(),
                LocalDateTime.now(),
                "Evaluation catalogue",
                "ROLE_ADMIN Admin Root",
                "Recalcul catalogue",
                List.of(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                true,
                "Score calcule",
                List.of(),
                "COMPLETED",
                "2.0.0",
                0.8,
                88.0,
                null,
                "PENDING",
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                "MANUAL_RECALCULATION",
                "corr-1",
                "multimodal-semantic-v2",
                "multimodal-semantic-v2",
                "multimodal-semantic-v2",
                "2026-07",
                "project-hash",
                "deliverable-hash",
                "input-hash",
                "PROCESSED",
                "ELIGIBLE"
        );
    }

    @Test
    void calculerScore_shouldUseCatalogueServiceMethod() {
        when(evaluationService.calculateAutomaticEvaluationForCatalogue(7L, "Recalcul manuel projet catalogue"))
                .thenReturn(response);

        EvaluationResponse result = controller.calculerScore(7L);

        assertEquals(response, result);
        verify(evaluationService).calculateAutomaticEvaluationForCatalogue(7L, "Recalcul manuel projet catalogue");
    }

    @Test
    void getLatestEvaluation_shouldUseCatalogueId() {
        when(evaluationService.getLatestEvaluationByProjetCatalogue(7L)).thenReturn(response);

        EvaluationResponse result = controller.getLatestEvaluation(7L);

        assertEquals(response, result);
        verify(evaluationService).getLatestEvaluationByProjetCatalogue(7L);
    }

    @Test
    void getEvaluationHistory_shouldUseCatalogueId() {
        when(evaluationService.getEvaluationHistoryByProjetCatalogue(7L)).thenReturn(List.of(response));

        List<EvaluationResponse> result = controller.getEvaluationHistory(7L);

        assertEquals(List.of(response), result);
        verify(evaluationService).getEvaluationHistoryByProjetCatalogue(7L);
    }

    @Test
    void getEvaluationDetail_shouldScopeByCatalogueId() {
        when(evaluationService.getEvaluationDetailByProjetCatalogue(7L, 10L)).thenReturn(response);

        EvaluationResponse result = controller.getEvaluationDetail(7L, 10L);

        assertEquals(response, result);
        verify(evaluationService).getEvaluationDetailByProjetCatalogue(7L, 10L);
    }
}
