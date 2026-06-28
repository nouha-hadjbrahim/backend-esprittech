package tn.esprit.backend_esprittech.controller.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationControllerTest {

    @Mock private EvaluationService evaluationService;
    @Mock private SujetProjetService sujetProjetService;

    @InjectMocks private EvaluationController controller;

    private EvaluationResponse evaluationResponse;
    private EvaluationRequest evaluationRequest;
    private SujetProjetResponse sujetProjetResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        evaluationResponse = new EvaluationResponse(
                1L, 1L, 8.5, true, false, now, "Évaluation completed", List.of()
        );
        
        evaluationRequest = new EvaluationRequest(
                new ArrayList<>(), new ArrayList<>()
        );

        sujetProjetResponse = new SujetProjetResponse(
                1L, "Titre", null, "Desc", "Obj", List.of("Pre"), List.of("Domaine"),
                List.of("Java"), 2, null, 8.5, false, false, 1001L, "Encadrant Test",
                now, now, null, null, null, null
        );
    }

    @Test
    void calculerEvaluation_withValidRequest_returnsEvaluationResponse() {
        when(evaluationService.calculerEvaluation(1L, evaluationRequest)).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.calculerEvaluation(1L, evaluationRequest);

        assertEquals(evaluationResponse, result);
        assertEquals(1L, result.sujetProjetId());
        assertEquals(8.5, result.scoreFinal());
        verify(evaluationService).calculerEvaluation(1L, evaluationRequest);
    }

    @Test
    void calculerEvaluation_differentProjetId_callsServiceWithCorrectId() {
        Long projectId = 999L;
        when(evaluationService.calculerEvaluation(projectId, evaluationRequest)).thenReturn(evaluationResponse);

        controller.calculerEvaluation(projectId, evaluationRequest);

        verify(evaluationService).calculerEvaluation(projectId, evaluationRequest);
    }

    @Test
    void getLatestEvaluation_withValidId_returnsEvaluationResponse() {
        when(evaluationService.getLatestEvaluationByProjet(1L)).thenReturn(evaluationResponse);

        EvaluationResponse result = controller.getLatestEvaluation(1L);

        assertEquals(evaluationResponse, result);
        assertEquals(8.5, result.scoreFinal());
        verify(evaluationService).getLatestEvaluationByProjet(1L);
    }

    @Test
    void getLatestEvaluation_differentId_callsServiceWithCorrectId() {
        Long projectId = 555L;
        when(evaluationService.getLatestEvaluationByProjet(projectId)).thenReturn(evaluationResponse);

        controller.getLatestEvaluation(projectId);

        verify(evaluationService).getLatestEvaluationByProjet(projectId);
    }

    @Test
    void getEvaluableProjects_returnsList() {
        List<SujetProjetResponse> projects = List.of(sujetProjetResponse);
        when(sujetProjetService.getProjetsEvaluables()).thenReturn(projects);

        List<SujetProjetResponse> result = controller.getEvaluableProjects();

        assertEquals(1, result.size());
        assertEquals(sujetProjetResponse, result.get(0));
        verify(sujetProjetService).getProjetsEvaluables();
    }

    @Test
    void getEvaluableProjects_emptyList_returnsEmptyList() {
        when(sujetProjetService.getProjetsEvaluables()).thenReturn(new ArrayList<>());

        List<SujetProjetResponse> result = controller.getEvaluableProjects();

        assertTrue(result.isEmpty());
        verify(sujetProjetService).getProjetsEvaluables();
    }

    @Test
    void getEvaluableProjects_multipleProjects_returnsAllProjects() {
        LocalDateTime now = LocalDateTime.now();
        List<SujetProjetResponse> projects = List.of(
                new SujetProjetResponse(1L, "Project1", null, "Desc1", "Obj1", List.of("Pre1"), List.of("Dom1"),
                        List.of("Java"), 2, null, 8.5, false, false, 1001L, "Encadrant1", now, now, null, null, null, null),
                new SujetProjetResponse(2L, "Project2", null, "Desc2", "Obj2", List.of("Pre2"), List.of("Dom2"),
                        List.of("Python"), 3, null, 7.5, false, false, 1002L, "Encadrant2", now, now, null, null, null, null)
        );
        when(sujetProjetService.getProjetsEvaluables()).thenReturn(projects);

        List<SujetProjetResponse> result = controller.getEvaluableProjects();

        assertEquals(2, result.size());
        verify(sujetProjetService).getProjetsEvaluables();
    }

    @Test
    void calculerEvaluation_multipleRequests_callsServiceForEach() {
        when(evaluationService.calculerEvaluation(1L, evaluationRequest)).thenReturn(evaluationResponse);
        when(evaluationService.calculerEvaluation(2L, evaluationRequest)).thenReturn(evaluationResponse);

        controller.calculerEvaluation(1L, evaluationRequest);
        controller.calculerEvaluation(2L, evaluationRequest);

        verify(evaluationService).calculerEvaluation(1L, evaluationRequest);
        verify(evaluationService).calculerEvaluation(2L, evaluationRequest);
    }

    @Test
    void getLatestEvaluation_multipleIds_callsServiceForEach() {
        EvaluationResponse response2 = new EvaluationResponse(2L, 2L, 7.5, false, true, LocalDateTime.now(), "Different", List.of());
        when(evaluationService.getLatestEvaluationByProjet(1L)).thenReturn(evaluationResponse);
        when(evaluationService.getLatestEvaluationByProjet(2L)).thenReturn(response2);

        controller.getLatestEvaluation(1L);
        controller.getLatestEvaluation(2L);

        verify(evaluationService).getLatestEvaluationByProjet(1L);
        verify(evaluationService).getLatestEvaluationByProjet(2L);
    }

    @Test
    void calculerEvaluation_withDifferentScores_returnsCorrectResponse() {
        EvaluationResponse response2 = new EvaluationResponse(2L, 2L, 9.8, true, false, LocalDateTime.now(), "Good", List.of());
        when(evaluationService.calculerEvaluation(2L, evaluationRequest)).thenReturn(response2);

        EvaluationResponse result = controller.calculerEvaluation(2L, evaluationRequest);

        assertEquals(9.8, result.scoreFinal());
    }
}
