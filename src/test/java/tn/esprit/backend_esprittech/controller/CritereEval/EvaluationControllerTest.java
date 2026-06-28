package tn.esprit.backend_esprittech.controller.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
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
                88.0, true, false, 1001L, "Encadrant 1", now, now, null, null, null, null
        );
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
    void getEvaluableProjects_shouldReturnProjectList() {
        when(sujetProjetService.getProjetsEvaluables()).thenReturn(List.of(sujetProjetResponse));

        List<SujetProjetResponse> result = controller.getEvaluableProjects();

        assertEquals(1, result.size());
        verify(sujetProjetService).getProjetsEvaluables();
    }
}
