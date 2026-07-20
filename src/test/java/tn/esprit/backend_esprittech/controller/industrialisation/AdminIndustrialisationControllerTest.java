package tn.esprit.backend_esprittech.controller.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.QuestionIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.QuestionIndustrialisationResponse;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationService;
import tn.esprit.backend_esprittech.service.industrialisation.QuestionIndustrialisationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminIndustrialisationControllerTest {

    @Mock
    private QuestionIndustrialisationService questionService;

    @Mock
    private IndustrialisationService industrialisationService;

    @InjectMocks
    private AdminIndustrialisationController controller;

    private QuestionIndustrialisationResponse questionResponse;
    private QuestionIndustrialisationRequest questionRequest;
    private CandidatureIndustrialisationResponse candidatureResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        questionResponse = new QuestionIndustrialisationResponse(
                1L,
                "Depot Git disponible ?",
                "Verifier le lien",
                TypeReponseIndustrialisation.BOOLEAN,
                true,
                TypeCritere.ELIMINATOIRE,
                null,
                1,
                true,
                true,
                now,
                now
        );
        questionRequest = new QuestionIndustrialisationRequest(
                "Depot Git disponible ?",
                "Verifier le lien",
                TypeReponseIndustrialisation.BOOLEAN,
                true,
                TypeCritere.ELIMINATOIRE,
                null,
                1,
                true,
                true
        );
        candidatureResponse = new CandidatureIndustrialisationResponse(
                11L,
                42L,
                "Plateforme IoT",
                StatutSujet.CANDIDAT_INDUSTRIALISATION_INTERNE,
                80.0,
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
    void findQuestions_shouldReturnAllQuestions() {
        when(questionService.findAll()).thenReturn(List.of(questionResponse));

        List<QuestionIndustrialisationResponse> result = controller.findQuestions();

        assertEquals(List.of(questionResponse), result);
        verify(questionService).findAll();
    }

    @Test
    void findActiveQuestions_shouldReturnActiveQuestions() {
        when(questionService.findActive()).thenReturn(List.of(questionResponse));

        List<QuestionIndustrialisationResponse> result = controller.findActiveQuestions();

        assertEquals(List.of(questionResponse), result);
        verify(questionService).findActive();
    }

    @Test
    void createQuestion_validRequest_shouldDelegateToService() {
        when(questionService.create(questionRequest)).thenReturn(questionResponse);

        QuestionIndustrialisationResponse result = controller.createQuestion(questionRequest);

        assertEquals(questionResponse, result);
        verify(questionService).create(questionRequest);
    }

    @Test
    void updateQuestion_validRequest_shouldDelegateToService() {
        when(questionService.update(1L, questionRequest)).thenReturn(questionResponse);

        QuestionIndustrialisationResponse result = controller.updateQuestion(1L, questionRequest);

        assertEquals(questionResponse, result);
        verify(questionService).update(1L, questionRequest);
    }

    @Test
    void activateQuestion_existingQuestion_shouldReturnNoContent() {
        var result = controller.activateQuestion(1L);

        assertEquals(204, result.getStatusCode().value());
        verify(questionService).activate(1L);
    }

    @Test
    void deactivateQuestion_existingQuestion_shouldReturnNoContent() {
        var result = controller.deactivateQuestion(1L);

        assertEquals(204, result.getStatusCode().value());
        verify(questionService).deactivate(1L);
    }

    @Test
    void deleteQuestion_existingQuestion_shouldDeactivateAndReturnNoContent() {
        var result = controller.deleteQuestion(1L);

        assertEquals(204, result.getStatusCode().value());
        verify(questionService).deactivate(1L);
    }

    @Test
    void findAllRequests_shouldReturnAdminRequests() {
        when(industrialisationService.listForAdmin()).thenReturn(List.of(candidatureResponse));

        List<CandidatureIndustrialisationResponse> result = controller.findAllRequests();

        assertEquals(List.of(candidatureResponse), result);
        verify(industrialisationService).listForAdmin();
    }
}
