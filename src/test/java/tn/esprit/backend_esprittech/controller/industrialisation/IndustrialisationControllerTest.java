package tn.esprit.backend_esprittech.controller.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.IndustrialisationFormResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.ReponsesIndustrialisationRequest;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustrialisationControllerTest {

    @Mock
    private IndustrialisationService industrialisationService;

    @InjectMocks
    private IndustrialisationController controller;

    private CandidatureIndustrialisationResponse candidatureResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        candidatureResponse = new CandidatureIndustrialisationResponse(
                11L,
                42L,
                "Plateforme IoT",
                StatutSujet.REALISATION_TERMINEE,
                82.0,
                true,
                false,
                TypeIndustrialisation.INTERNE,
                StatutIndustrialisation.BROUILLON,
                7L,
                "Ali Mansour",
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Demande",
                List.of(),
                List.of(),
                List.of()
        );
    }

    @Test
    void create_validRequest_shouldDelegateToService() {
        CandidatureIndustrialisationRequest request = new CandidatureIndustrialisationRequest(TypeIndustrialisation.INTERNE, "Demande");
        when(industrialisationService.create(42L, request)).thenReturn(candidatureResponse);

        CandidatureIndustrialisationResponse result = controller.create(42L, request);

        assertEquals(candidatureResponse, result);
        verify(industrialisationService).create(42L, request);
    }

    @Test
    void formulaire_existingCandidature_shouldReturnForm() {
        IndustrialisationFormResponse form = new IndustrialisationFormResponse(candidatureResponse, List.of(), List.of());
        when(industrialisationService.getFormulaire(11L)).thenReturn(form);

        IndustrialisationFormResponse result = controller.formulaire(11L);

        assertEquals(form, result);
        verify(industrialisationService).getFormulaire(11L);
    }

    @Test
    void detail_existingCandidature_shouldReturnDetail() {
        when(industrialisationService.detail(11L)).thenReturn(candidatureResponse);

        CandidatureIndustrialisationResponse result = controller.detail(11L);

        assertEquals(candidatureResponse, result);
        verify(industrialisationService).detail(11L);
    }

    @Test
    void saveReponses_validPayload_shouldDelegateToService() {
        ReponsesIndustrialisationRequest request = new ReponsesIndustrialisationRequest(List.of());
        when(industrialisationService.saveReponses(11L, request)).thenReturn(candidatureResponse);

        CandidatureIndustrialisationResponse result = controller.saveReponses(11L, request);

        assertEquals(candidatureResponse, result);
        verify(industrialisationService).saveReponses(11L, request);
    }

    @Test
    void uploadPreuve_validFile_shouldDelegateToService() {
        MockMultipartFile file = new MockMultipartFile("file", "preuve.pdf", "application/pdf", "data".getBytes());
        when(industrialisationService.uploadPreuve(11L, 5L, file)).thenReturn(candidatureResponse);

        CandidatureIndustrialisationResponse result = controller.uploadPreuve(11L, 5L, file);

        assertEquals(candidatureResponse, result);
        verify(industrialisationService).uploadPreuve(11L, 5L, file);
    }

    @Test
    void soumettre_existingDraft_shouldDelegateToService() {
        CandidatureIndustrialisationResponse submitted = new CandidatureIndustrialisationResponse(
                candidatureResponse.id(),
                candidatureResponse.projetId(),
                candidatureResponse.projetTitre(),
                candidatureResponse.projetStatut(),
                candidatureResponse.scoreEvaluationProjet(),
                candidatureResponse.eligibleIndustrialisation(),
                candidatureResponse.bloqueParEliminatoire(),
                candidatureResponse.typeIndustrialisation(),
                StatutIndustrialisation.SOUMISE,
                candidatureResponse.demandeurId(),
                candidatureResponse.demandeurNom(),
                candidatureResponse.dateDemande(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                candidatureResponse.commentaire(),
                List.of(),
                List.of(),
                List.of()
        );
        when(industrialisationService.soumettre(11L)).thenReturn(submitted);

        CandidatureIndustrialisationResponse result = controller.soumettre(11L);

        assertEquals(StatutIndustrialisation.SOUMISE, result.statut());
        verify(industrialisationService).soumettre(11L);
    }

    @Test
    void mesDemandes_currentTeacher_shouldReturnRequests() {
        when(industrialisationService.mesDemandes()).thenReturn(List.of(candidatureResponse));

        List<CandidatureIndustrialisationResponse> result = controller.mesDemandes();

        assertEquals(List.of(candidatureResponse), result);
        verify(industrialisationService).mesDemandes();
    }
}
