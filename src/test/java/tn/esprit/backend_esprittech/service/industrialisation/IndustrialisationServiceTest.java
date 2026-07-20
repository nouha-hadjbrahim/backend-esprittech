package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionGoRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionNoGoRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.ReponseIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.ReponsesIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.SubmitIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatCritereResponse;
import tn.esprit.backend_esprittech.dto.livrable.CataloguePublicLivrableResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.OrientationIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.EliminatoryWarningsConfirmationRequiredException;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.dto.response.HistoriqueResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.service.historique.HistoriqueService;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;
import tn.esprit.backend_esprittech.service.livrable.LivrableCatalogueService;
import tn.esprit.backend_esprittech.service.livrable.LivrableService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class IndustrialisationServiceTest {

    @Mock
    private CandidatureIndustrialisationRepository candidatureRepository;

    @Mock
    private QuestionIndustrialisationRepository questionRepository;

    @Mock
    private ReponseIndustrialisationRepository reponseRepository;

    @Mock
    private HistoriqueService historiqueService;

    @Mock
    private SujetProjetRepository sujetProjetRepository;

    @Mock
    private UserService userService;

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private LivrableService livrableService;

    @Mock
    private MinioStorageService storageService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjetCatalogueRepository projetCatalogueRepository;

    @Mock
    private LivrableCatalogueRepository livrableCatalogueRepository;

    @Mock
    private LivrableCatalogueService livrableCatalogueService;

    @InjectMocks
    private IndustrialisationService industrialisationService;

    private User encadrant;
    private User ci;
    private SujetProjet projet;
    private CandidatureIndustrialisation candidature;

    @BeforeEach
    void setUp() {
        encadrant = new User();
        encadrant.setId(7L);
        encadrant.setPrenom("Ali");
        encadrant.setNom("Mansour");
        encadrant.setRole(Role.ROLE_ENSEIGNANT);

        ci = new User();
        ci.setId(9L);
        ci.setPrenom("Comite");
        ci.setNom("Industrie");
        ci.setRole(Role.ROLE_CI);

        projet = new SujetProjet();
        projet.setId(42L);
        projet.setTitre("Plateforme IoT");
        projet.setEncadrantId(7L);
        projet.setStatut(StatutSujet.REALISATION_TERMINEE);
        projet.setScoreFinalEvaluation(76.0);
        projet.setEligibleIndustrialisation(true);
        projet.setBloqueParEliminatoire(false);

        candidature = new CandidatureIndustrialisation();
        candidature.setId(11L);
        candidature.setSujetProjet(projet);
        candidature.setDemandeur(encadrant);
        candidature.setTypeIndustrialisation(TypeIndustrialisation.INTERNE);
        candidature.setStatut(StatutIndustrialisation.BROUILLON);
    }

    @Test
    void soumettre_eliminatoryNotOkWithoutConfirmation_shouldRequireConfirmation() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = responseFor(question);
        reponse.setValeurBoolean(false);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));

        EliminatoryWarningsConfirmationRequiredException ex = assertThrows(EliminatoryWarningsConfirmationRequiredException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals(1, ex.getCount());
        assertEquals("Depot Git disponible ?", ex.getDetails().get(0).libelle());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void soumettre_eliminatoryNotOkWithConfirmation_shouldSubmitWithWarnings() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = responseFor(question);
        reponse.setValeurBoolean(false);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(livrableService.findByProjet(42L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(userRepository.findByRole(Role.ROLE_CI)).thenReturn(List.of(ci));

        var response = industrialisationService.soumettre(11L, new SubmitIndustrialisationRequest(true));

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
        assertTrue(response.hasEliminatoryWarnings());
        assertEquals(1, response.eliminatoryWarningsCount());
        assertTrue(response.eliminatoryWarningsDetails().get(0).contains("Depot Git disponible"));
        verify(notificationService, atLeastOnce()).creerNotification(
                eq(9L), eq(NotificationType.CI_DEMANDE_SOUMISE),
                eq("Demande d'industrialisation"), anyString(),
                eq("/ci/industrialisation"));
    }

    @Test
    void create_existingDraftRequest_shouldReopenDraft() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of(candidature));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(livrableService.findByProjet(42L)).thenReturn(List.of());

        var response = industrialisationService.create(42L, new CandidatureIndustrialisationRequest(TypeIndustrialisation.EXTERNE, null));

        assertEquals(11L, response.id());
        assertEquals(StatutIndustrialisation.BROUILLON, response.statut());
    }

    @Test
    void create_existingSubmittedRequest_shouldRejectDuplicate() {
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.create(42L, new CandidatureIndustrialisationRequest(TypeIndustrialisation.EXTERNE, null)));

        assertEquals("Une demande d'industrialisation est deja en cours pour ce projet.", ex.getMessage());
    }

    @Test
    void create_newRequest_shouldPersistSnapshotAndHistory() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of());
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> {
            CandidatureIndustrialisation saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });
        stubDetails(12L, 42L);

        var response = industrialisationService.create(42L,
                new CandidatureIndustrialisationRequest(TypeIndustrialisation.EXTERNE, "  besoin client  "));

        ArgumentCaptor<CandidatureIndustrialisation> captor = ArgumentCaptor.forClass(CandidatureIndustrialisation.class);
        verify(candidatureRepository).save(captor.capture());
        assertEquals(12L, response.id());
        assertEquals(TypeIndustrialisation.EXTERNE, captor.getValue().getTypeIndustrialisation());
        assertEquals("besoin client", captor.getValue().getCommentaire());
        assertEquals(76.0, captor.getValue().getScoreEvaluationProjet());
    }

    @Test
    void create_nonOwnerTeacher_shouldReject() {
        User otherTeacher = user(99L, Role.ROLE_ENSEIGNANT);
        when(userService.getCurrentUser()).thenReturn(otherTeacher);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class,
                () -> industrialisationService.create(42L, new CandidatureIndustrialisationRequest(TypeIndustrialisation.INTERNE, null)));

        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void create_projectNotTerminated_shouldReject() {
        projet.setStatut(StatutSujet.REALISATION_EN_COURS);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.create(42L, new CandidatureIndustrialisationRequest(TypeIndustrialisation.INTERNE, null)));

        assertEquals("Le projet doit etre termine avant une demande d'industrialisation.", ex.getMessage());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void create_projectEvaluationWarningWithoutConfirmation_shouldRequireConfirmation() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of());
        when(evaluationService.getLatestEvaluationByProjet(42L)).thenReturn(evaluationWithWarning());

        EliminatoryWarningsConfirmationRequiredException ex = assertThrows(EliminatoryWarningsConfirmationRequiredException.class,
                () -> industrialisationService.create(42L,
                        new CandidatureIndustrialisationRequest(TypeIndustrialisation.INTERNE, null)));

        assertEquals(1, ex.getCount());
        assertEquals("Licence incompatible", ex.getDetails().get(0).libelle());
        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void create_projectEvaluationWarningWithConfirmation_shouldCreateDraftWithWarnings() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of());
        when(evaluationService.getLatestEvaluationByProjet(42L)).thenReturn(evaluationWithWarning());
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> {
            CandidatureIndustrialisation saved = invocation.getArgument(0);
            saved.setId(14L);
            return saved;
        });
        stubDetails(14L, 42L);

        var response = industrialisationService.create(42L,
                new CandidatureIndustrialisationRequest(TypeIndustrialisation.INTERNE, null, true));

        assertEquals(14L, response.id());
        assertTrue(response.hasEliminatoryWarnings());
        assertEquals(1, response.eliminatoryWarningsCount());
        assertTrue(response.eliminatoryWarningsDetails().get(0).contains("Licence incompatible"));
    }

    @Test
    void create_closedExistingRequest_shouldCreateNewRequest() {
        candidature.setStatut(StatutIndustrialisation.NO_GO);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of(candidature));
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> {
            CandidatureIndustrialisation saved = invocation.getArgument(0);
            saved.setId(13L);
            return saved;
        });
        stubDetails(13L, 42L);

        var response = industrialisationService.create(42L,
                new CandidatureIndustrialisationRequest(TypeIndustrialisation.INTERNE, null));

        assertEquals(13L, response.id());
        verify(candidatureRepository).save(any(CandidatureIndustrialisation.class));
    }

    @Test
    void getFormulaire_admin_shouldReturnQuestionsAndResponses() {
        User admin = user(1L, Role.ROLE_ADMIN);
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = responseFor(question);
        reponse.setValeurBoolean(true);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));
        when(livrableService.findByProjet(42L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        var response = industrialisationService.getFormulaire(11L);

        assertEquals(11L, response.candidature().id());
        assertEquals(1, response.questions().size());
        assertEquals(1, response.reponses().size());
    }

    @Test
    void saveReponses_newBooleanResponse_shouldComputeEliminatoryAndPersist() {
        QuestionIndustrialisation question = gitQuestion();
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationIdAndQuestionId(11L, 5L)).thenReturn(Optional.empty());
        when(candidatureRepository.save(candidature)).thenReturn(candidature);
        stubDetails(11L, 42L);

        industrialisationService.saveReponses(11L, new ReponsesIndustrialisationRequest(List.of(
                new ReponseIndustrialisationRequest(5L, null, false, null, null, ReponseEliminatoire.OK, 20.0, "  justification  ")
        )));

        ArgumentCaptor<ReponseIndustrialisation> captor = ArgumentCaptor.forClass(ReponseIndustrialisation.class);
        verify(reponseRepository).save(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getValeurBoolean());
        assertEquals(ReponseEliminatoire.NOT_OK, captor.getValue().getReponseEliminatoire());
        assertEquals(20.0, captor.getValue().getNoteObtenue());
        assertEquals("justification", captor.getValue().getJustificatif());
    }

    @Test
    void saveReponses_inactiveQuestion_shouldReject() {
        QuestionIndustrialisation question = gitQuestion();
        question.setActif(false);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));

        assertThrows(tn.esprit.backend_esprittech.exception.ResourceNotFoundException.class,
                () -> industrialisationService.saveReponses(11L, new ReponsesIndustrialisationRequest(List.of(
                        new ReponseIndustrialisationRequest(5L, "x", null, null, null, null, null, null)
                ))));
    }

    @Test
    void saveReponses_nonEditableStatus_shouldReject() {
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.saveReponses(11L, new ReponsesIndustrialisationRequest(List.of())));

        assertEquals("La demande n'est plus editable.", ex.getMessage());
    }

    @Test
    void uploadPreuve_validFile_shouldStoreProofOnResponse() {
        QuestionIndustrialisation question = gitQuestion();
        MockMultipartFile file = new MockMultipartFile(
                "file", "preuve git.pdf", "application/pdf", "preuve".getBytes());
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));
        when(storageService.upload(anyString(), any())).thenReturn(
                new MinioStorageService.StoredObject("industrialisation-preuves/11/5/proof.pdf", "application/pdf", 6L));
        when(reponseRepository.findByCandidatureIndustrialisationIdAndQuestionId(11L, 5L)).thenReturn(Optional.empty());
        when(candidatureRepository.save(candidature)).thenReturn(candidature);
        stubDetails(11L, 42L);

        industrialisationService.uploadPreuve(11L, 5L, file);

        ArgumentCaptor<ReponseIndustrialisation> captor = ArgumentCaptor.forClass(ReponseIndustrialisation.class);
        verify(storageService).upload(anyString(), any());
        verify(reponseRepository).save(captor.capture());
        assertEquals("industrialisation-preuves/11/5/proof.pdf", captor.getValue().getPreuveObjectName());
        assertEquals("preuve git.pdf", captor.getValue().getPreuveOriginalFileName());
        assertEquals("application/pdf", captor.getValue().getPreuveContentType());
        assertEquals(6L, captor.getValue().getPreuveSize());
    }

    @Test
    void soumettre_requiredFileQuestionWithoutProof_shouldRejectIncompleteForm() {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setId(5L);
        question.setLibelle("Preuve de depot");
        question.setTypeReponse(TypeReponseIndustrialisation.FICHIER);
        question.setTypeCritere(TypeCritere.ELIMINATOIRE);
        question.setObligatoire(true);
        question.setActif(true);
        question.setOrdre(1);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals("Reponse obligatoire manquante: Preuve de depot", ex.getMessage());
    }

    @Test
    void soumettre_requiredBooleanEliminatoryAnswer_shouldSubmitWhenPersisted() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();
        reponse.setId(22L);
        reponse.setCandidatureIndustrialisation(candidature);
        reponse.setQuestion(question);
        reponse.setValeurBoolean(true);
        reponse.setReponseEliminatoire(ReponseEliminatoire.OK);
        reponse.setJustificatif("Depot Git fourni");

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(userRepository.findByRole(Role.ROLE_CI)).thenReturn(List.of(ci));

        var response = industrialisationService.soumettre(11L);

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
        verify(notificationService, atLeastOnce()).creerNotification(
                eq(9L), eq(NotificationType.CI_DEMANDE_SOUMISE),
                eq("Demande d'industrialisation"), anyString(),
                eq("/ci/industrialisation"));
    }

    @Test
    void soumettre_requiredBooleanWithoutValue_shouldReturnPreciseError() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();
        reponse.setId(22L);
        reponse.setCandidatureIndustrialisation(candidature);
        reponse.setQuestion(question);
        reponse.setReponseEliminatoire(ReponseEliminatoire.OK);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals("Reponse booleenne manquante pour: Depot Git disponible ?", ex.getMessage());
    }

    @Test
    void soumettre_withoutProjectScore_shouldReject() {
        projet.setScoreFinalEvaluation(null);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals("Le projet doit etre evalue avant soumission a la CI.", ex.getMessage());
    }

    @Test
    void soumettre_withoutLivrables_shouldSubmitWithWarning() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(false);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(candidature)).thenReturn(candidature);
        when(livrableService.findByProjet(42L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(userRepository.findByRole(Role.ROLE_CI)).thenReturn(List.of(ci));

        var response = industrialisationService.soumettre(11L);

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
        assertTrue(response.warnings().contains("Aucun livrable nest déposé pour ce projet. La CI verra cette alerte."));
        verify(notificationService, atLeastOnce()).creerNotification(
                eq(9L), eq(NotificationType.CI_DEMANDE_SOUMISE),
                eq("Demande d'industrialisation"), anyString(),
                eq("/ci/industrialisation"));
    }

    @Test
    void soumettre_externalCandidate_shouldSetExternalProjectStatus() {
        candidature.setTypeIndustrialisation(TypeIndustrialisation.EXTERNE);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(candidature)).thenReturn(candidature);
        when(livrableService.findByProjet(42L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(userRepository.findByRole(Role.ROLE_CI)).thenReturn(List.of(ci));

        var response = industrialisationService.soumettre(11L);

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
        assertEquals(StatutSujet.CANDIDAT_INDUSTRIALISATION_EXTERNE, projet.getStatut());
        verify(notificationService, atLeastOnce()).creerNotification(
                eq(9L), eq(NotificationType.CI_DEMANDE_SOUMISE),
                eq("Demande d'industrialisation"), anyString(),
                eq("/ci/industrialisation"));
    }

    @Test
    void soumettre_requiredTextWithoutValue_shouldRejectWithTextMessage() {
        QuestionIndustrialisation question = requiredQuestion(TypeReponseIndustrialisation.TEXTE, "Description solution");
        ReponseIndustrialisation reponse = responseFor(question);
        reponse.setValeurTexte("   ");
        assertSubmissionMissingAnswer(question, reponse, "Reponse texte manquante pour: Description solution");
    }

    @Test
    void soumettre_requiredChoiceWithoutValue_shouldRejectWithTextMessage() {
        QuestionIndustrialisation question = requiredQuestion(TypeReponseIndustrialisation.CHOIX, "Mode de deploiement");
        ReponseIndustrialisation reponse = responseFor(question);
        assertSubmissionMissingAnswer(question, reponse, "Reponse texte manquante pour: Mode de deploiement");
    }

    @Test
    void soumettre_requiredNumericWithoutValue_shouldRejectWithNumericMessage() {
        QuestionIndustrialisation question = requiredQuestion(TypeReponseIndustrialisation.NUMERIQUE, "Nombre utilisateurs");
        ReponseIndustrialisation reponse = responseFor(question);
        assertSubmissionMissingAnswer(question, reponse, "Reponse numerique manquante pour: Nombre utilisateurs");
    }

    @Test
    void soumettre_requiredUrlWithoutValue_shouldRejectWithUrlMessage() {
        QuestionIndustrialisation question = requiredQuestion(TypeReponseIndustrialisation.URL, "URL demo");
        ReponseIndustrialisation reponse = responseFor(question);
        assertSubmissionMissingAnswer(question, reponse, "URL manquante pour: URL demo");
    }

    @Test
    void soumettre_requiredEliminatoryWithoutOkNotOk_shouldComputeAutomatically() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();
        reponse.setId(22L);
        reponse.setCandidatureIndustrialisation(candidature);
        reponse.setQuestion(question);
        reponse.setValeurBoolean(true);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(userRepository.findByRole(Role.ROLE_CI)).thenReturn(List.of(ci));

        var response = industrialisationService.soumettre(11L);

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
        verify(notificationService, atLeastOnce()).creerNotification(
                eq(9L), eq(NotificationType.CI_DEMANDE_SOUMISE),
                eq("Demande d'industrialisation"), anyString(),
                eq("/ci/industrialisation"));
    }

    @Test
    void mesDemandes_currentUser_shouldReturnOwnRequests() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findByDemandeurIdOrderByDateDemandeDesc(7L)).thenReturn(List.of(candidature));

        var responses = industrialisationService.mesDemandes();

        assertEquals(1, responses.size());
        assertEquals(11L, responses.get(0).id());
        assertTrue(responses.get(0).reponses().isEmpty());
    }

    @Test
    void listForCi_withoutStatus_shouldFilterByType() {
        CandidatureIndustrialisation external = new CandidatureIndustrialisation();
        external.setId(12L);
        external.setSujetProjet(projet);
        external.setDemandeur(encadrant);
        external.setTypeIndustrialisation(TypeIndustrialisation.EXTERNE);
        external.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findByStatutInOrderByDateDemandeDesc(any())).thenReturn(List.of(candidature, external));

        var responses = industrialisationService.listForCi(null, TypeIndustrialisation.EXTERNE);

        assertEquals(1, responses.size());
        assertEquals(12L, responses.get(0).id());
    }

    @Test
    void listForCi_withStatus_shouldUseRequestedStatus() {
        candidature.setStatut(StatutIndustrialisation.GO);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findByStatutInOrderByDateDemandeDesc(List.of(StatutIndustrialisation.GO)))
                .thenReturn(List.of(candidature));

        var responses = industrialisationService.listForCi(StatutIndustrialisation.GO, null);

        assertEquals(1, responses.size());
        assertEquals(StatutIndustrialisation.GO, responses.get(0).statut());
    }

    @Test
    void listForCi_nonCiUser_shouldReject() {
        when(userService.getCurrentUser()).thenReturn(encadrant);

        assertThrows(AccessDeniedException.class,
                () -> industrialisationService.listForCi(null, null));
    }

    @Test
    void listForAdmin_admin_shouldReturnRequests() {
        User admin = user(1L, Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(candidatureRepository.findAllByOrderByDateDemandeDesc()).thenReturn(List.of(candidature));

        var responses = industrialisationService.listForAdmin();

        assertEquals(1, responses.size());
        assertEquals(11L, responses.get(0).id());
    }

    @Test
    void listForAdmin_nonAdmin_shouldReject() {
        when(userService.getCurrentUser()).thenReturn(ci);

        assertThrows(AccessDeniedException.class, () -> industrialisationService.listForAdmin());
    }

    @Test
    void detail_owner_shouldReturnDetails() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        stubDetails(11L, 42L);

        var response = industrialisationService.detail(11L);

        assertEquals(11L, response.id());
    }

    @Test
    void detail_withHistory_shouldMapActorAndTransitionFields() {
        LocalDateTime transitionDate = LocalDateTime.of(2026, 2, 1, 10, 30);
        HistoriqueResponse history = new HistoriqueResponse(
                70L, 9L, "Industrie", "Comite", Role.ROLE_CI,
                tn.esprit.backend_esprittech.enums.ActionType.DECISION_GO,
                EntityType.CANDIDATURE_INDUSTRIALISATION, 11L,
                "Décision GO - DSI",
                "{\"statut\":\"SOUMISE\"}",
                "{\"statut\":\"GO\",\"orientation\":\"DSI\"}",
                "{\"commentaire\":\"Go DSI\"}",
                transitionDate
        );

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(livrableService.findByProjet(42L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(history), 0, 20, 1, 1, true, true));

        var response = industrialisationService.detail(11L);

        assertEquals(1, response.historique().size());
        var mapped = response.historique().get(0);
        assertEquals(70L, mapped.id());
        assertEquals(StatutIndustrialisation.SOUMISE, mapped.statutSource());
        assertEquals(StatutIndustrialisation.GO, mapped.statutCible());
        assertEquals(9L, mapped.acteurId());
        assertEquals("Comite Industrie", mapped.acteurNom());
        assertEquals(Role.ROLE_CI, mapped.roleActeur());
        assertEquals(transitionDate, mapped.dateTransition());
        assertEquals("Go DSI", mapped.commentaire());
    }

    @Test
    void detail_nonOwnerTeacher_shouldReject() {
        when(userService.getCurrentUser()).thenReturn(user(99L, Role.ROLE_ENSEIGNANT));
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        assertThrows(AccessDeniedException.class, () -> industrialisationService.detail(11L));
    }

    @Test
    void detailForCi_nonCi_shouldReject() {
        when(userService.getCurrentUser()).thenReturn(encadrant);

        assertThrows(AccessDeniedException.class, () -> industrialisationService.detailForCi(11L));
    }

    @Test
    void decideGo_validDsiOrientation_shouldUpdateProjectAndDecision() {
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(candidature)).thenReturn(candidature);
        stubDetails(11L, 42L);

        var response = industrialisationService.decideGo(11L,
                new DecisionGoRequest(OrientationIndustrialisation.DSI, "  ok dsi  "));

        assertEquals(StatutIndustrialisation.GO, response.statut());
        assertTrue(candidature.getDecisionGoNoGo());
        assertEquals(OrientationIndustrialisation.DSI, candidature.getOrientation());
        assertEquals("ok dsi", candidature.getMotifDecision());
        assertEquals(StatutSujet.INDUSTRIALISE_DSI, projet.getStatut());
        verify(notificationService).creerNotification(
                eq(7L), eq(NotificationType.CI_DEMANDE_APPOUVEE),
                eq("Demande industrialisation approuvee"), anyString(),
                eq("/frontoffice/demandes-industrialisation"));
    }

    @Test
    void decideGo_validPartnerOrientation_shouldSetExternalIndustrialisationStatus() {
        candidature.setStatut(StatutIndustrialisation.RECEVABLE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(candidature)).thenReturn(candidature);
        stubDetails(11L, 42L);

        industrialisationService.decideGo(11L,
                new DecisionGoRequest(OrientationIndustrialisation.ENTREPRISE_PARTENAIRE, null));

        assertEquals(StatutSujet.INDUSTRIALISE_EXTERNE, projet.getStatut());
        assertNull(candidature.getMotifDecision());
        verify(notificationService).creerNotification(
                eq(7L), eq(NotificationType.CI_DEMANDE_APPOUVEE),
                eq("Demande industrialisation approuvee"), anyString(),
                eq("/frontoffice/demandes-industrialisation"));
    }

    @Test
    void decideGo_withoutOrientation_shouldReject() {
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.decideGo(11L, new DecisionGoRequest(null, null)));

        assertEquals("L'orientation est obligatoire pour une decision Go.", ex.getMessage());
    }

    @Test
    void decideGo_nonPendingStatus_shouldReject() {
        candidature.setStatut(StatutIndustrialisation.BROUILLON);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.decideGo(11L, new DecisionGoRequest(OrientationIndustrialisation.DSI, null)));

        assertEquals("La demande n'est pas en attente de decision CI.", ex.getMessage());
    }

    @Test
    void decideGo_nonCiUser_shouldReject() {
        when(userService.getCurrentUser()).thenReturn(encadrant);

        assertThrows(AccessDeniedException.class,
                () -> industrialisationService.decideGo(11L, new DecisionGoRequest(OrientationIndustrialisation.DSI, null)));
    }

    @Test
    void decideNoGo_blankMotif_shouldRequireMotif() {
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.decideNoGo(11L, new DecisionNoGoRequest("  ")));

        assertEquals("Le motif est obligatoire pour une decision No Go.", ex.getMessage());
    }

    @Test
    void decideNoGo_validMotif_shouldPersistDecision() {
        candidature.setStatut(StatutIndustrialisation.RECUE_PAR_CI);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(candidatureRepository.save(candidature)).thenReturn(candidature);
        stubDetails(11L, 42L);

        var response = industrialisationService.decideNoGo(11L, new DecisionNoGoRequest("  manque de maturite  "));

        assertEquals(StatutIndustrialisation.NO_GO, response.statut());
        assertFalse(candidature.getDecisionGoNoGo());
        assertEquals("manque de maturite", candidature.getMotifDecision());
        assertEquals(ci, candidature.getCiDecideur());
        verify(notificationService).creerNotification(
                eq(7L), eq(NotificationType.CI_DEMANDE_REJETEE),
                eq("Demande industrialisation refusee"), anyString(),
                eq("/frontoffice/demandes-industrialisation"));
    }

    @Test
    void detailForCi_shouldExposeLatestProjectScore() {
        projet.setScoreFinalEvaluation(91.0);
        candidature.setScoreEvaluationProjet(55.0);
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 11L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(livrableService.findByProjet(42L)).thenReturn(List.of(
                new LivrableResponse(1L, 42L, "Plateforme IoT", null, "Rapport", null, null, null, null, null, null, 7L, "Ali Mansour", null, true)
        ));

        var response = industrialisationService.detailForCi(11L);

        assertEquals(91.0, response.scoreEvaluationProjet());
    }

    private void assertSubmissionMissingAnswer(QuestionIndustrialisation question,
                                               ReponseIndustrialisation reponse,
                                               String expectedMessage) {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals(expectedMessage, ex.getMessage());
    }

    private void stubDetails(Long candidatureId, Long projetId) {
        when(reponseRepository.findByCandidatureIndustrialisationId(candidatureId)).thenReturn(List.of());
        when(livrableService.findByProjet(projetId)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, candidatureId, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setPrenom("User");
        user.setNom(String.valueOf(id));
        user.setRole(role);
        return user;
    }

    private QuestionIndustrialisation requiredQuestion(TypeReponseIndustrialisation type, String libelle) {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setId(50L + type.ordinal());
        question.setLibelle(libelle);
        question.setTypeReponse(type);
        question.setTypeCritere(TypeCritere.NOTE);
        question.setObligatoire(true);
        question.setActif(true);
        question.setOrdre(type.ordinal() + 1);
        return question;
    }

    private ReponseIndustrialisation responseFor(QuestionIndustrialisation question) {
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();
        reponse.setId(100L + question.getId());
        reponse.setCandidatureIndustrialisation(candidature);
        reponse.setQuestion(question);
        return reponse;
    }

    private QuestionIndustrialisation gitQuestion() {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setId(5L);
        question.setLibelle("Depot Git disponible ?");
        question.setTypeReponse(TypeReponseIndustrialisation.BOOLEAN);
        question.setTypeCritere(TypeCritere.ELIMINATOIRE);
        question.setObligatoire(true);
        question.setActif(true);
        question.setOrdre(1);
        return question;
    }

    private EvaluationResponse evaluationWithWarning() {
        ResultatCritereResponse warning = new ResultatCritereResponse(
                91L,
                77L,
                "Licence incompatible",
                "Licence non compatible",
                "Juridique",
                TypeCritere.ELIMINATOIRE,
                null,
                ReponseEliminatoire.NOT_OK,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "projectStatus=REALISATION_TERMINEE",
                "Critere eliminatoire projet NOT_OK.",
                "Licence detectee",
                List.of(),
                true,
                "Licence detectee"
        );
        return new EvaluationResponse(
                90L,
                42L,
                88.0,
                true,
                true,
                true,
                1,
                List.of("Licence incompatible"),
                LocalDateTime.now(),
                "Projet evalue avec alertes",
                "SYSTEM",
                "test",
                List.of(warning),
                0,
                0,
                0,
                1,
                0,
                0,
                true,
                "Score calcule",
                List.of("Licence incompatible")
        );
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsCandidatureFields() {
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        var result = industrialisationService.getOldValues(11L);

        assertEquals(candidature.getStatut().name(), result.get("statut"));
    }

    // ---- getProjetTitreById ----

    @Test
    void getProjetTitreById_found_returnsTitre() {
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        assertEquals("Plateforme IoT", industrialisationService.getProjetTitreById(42L));
    }

    @Test
    void getProjetTitreById_notFound_returnsInconnu() {
        when(sujetProjetRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("Inconnu", industrialisationService.getProjetTitreById(99L));
    }

    // ---- getProjetTitreByCandidatureId ----

    @Test
    void getProjetTitreByCandidatureId_found_returnsTitre() {
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        assertEquals("Plateforme IoT", industrialisationService.getProjetTitreByCandidatureId(11L));
    }

    @Test
    void getProjetTitreByCandidatureId_notFound_returnsInconnu() {
        when(candidatureRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("Inconnu", industrialisationService.getProjetTitreByCandidatureId(99L));
    }

    @Test
    void createForCatalogue_shouldAssociateProjetAndCopyScoreSnapshot() {
        ProjetCatalogue catalogue = new ProjetCatalogue();
        catalogue.setId(55L);
        catalogue.setTitre("Catalogue IoT");
        catalogue.setStatut(StatutProjet.VALIDE);
        catalogue.setTypeProjet(TypeProjet.RDI);
        catalogue.setScore(84);
        catalogue.setSujetId(42L);
        catalogue.setEncadrant(encadrant);
        catalogue.setDomaines(java.util.Set.of());
        catalogue.setTechnologies(java.util.Set.of());
        catalogue.setPrerequis(java.util.Set.of());

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(projetCatalogueRepository.findById(55L)).thenReturn(Optional.of(catalogue));
        when(candidatureRepository.findByProjetCatalogueIdOrderByDateDemandeDesc(55L)).thenReturn(List.of());
        when(evaluationService.getLatestEvaluationByProjetCatalogue(55L))
                .thenThrow(new tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException("none"));
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> {
            CandidatureIndustrialisation saved = invocation.getArgument(0);
            saved.setId(18L);
            return saved;
        });
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 18L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(livrableCatalogueService.findPublishedForPublicCatalogue(55L)).thenReturn(List.of());

        var response = industrialisationService.createForCatalogue(
                55L, new CandidatureIndustrialisationRequest(TypeIndustrialisation.INTERNE, null, false));

        assertEquals(18L, response.id());
        assertEquals(55L, response.projetId());
        assertEquals(84.0, response.scoreEvaluationProjet());
        assertNull(response.latestEvaluation());
        ArgumentCaptor<CandidatureIndustrialisation> captor = ArgumentCaptor.forClass(CandidatureIndustrialisation.class);
        verify(candidatureRepository).save(captor.capture());
        assertEquals(catalogue, captor.getValue().getProjetCatalogue());
        assertNull(captor.getValue().getSujetProjet());
        assertEquals(84.0, captor.getValue().getScoreEvaluationProjet());
    }

    @Test
    void detailForCi_convertedCandidature_shouldExposeCatalogueEvaluation() {
        ProjetCatalogue catalogue = new ProjetCatalogue();
        catalogue.setId(55L);
        catalogue.setTitre("Catalogue IoT");
        catalogue.setStatut(StatutProjet.VALIDE);
        catalogue.setTypeProjet(TypeProjet.RDI);
        catalogue.setScore(0);
        catalogue.setSujetId(42L);
        catalogue.setEncadrant(encadrant);
        catalogue.setDomaines(java.util.Set.of());
        catalogue.setTechnologies(java.util.Set.of());
        catalogue.setPrerequis(java.util.Set.of());

        CandidatureIndustrialisation converted = new CandidatureIndustrialisation();
        converted.setId(18L);
        converted.setProjetCatalogue(catalogue);
        converted.setSujetProjet(null);
        converted.setTypeIndustrialisation(TypeIndustrialisation.INTERNE);
        converted.setStatut(StatutIndustrialisation.SOUMISE);
        converted.setDemandeur(encadrant);
        converted.setDateDemande(LocalDateTime.now());
        converted.setScoreEvaluationProjet(81.0);

        EvaluationResponse evaluation = new EvaluationResponse(
                9L,
                42L,
                81.0,
                true,
                false,
                false,
                0,
                List.of(),
                LocalDateTime.now(),
                null,
                "SYSTEM",
                null,
                List.of(),
                1,
                1,
                0,
                0,
                0,
                0,
                true,
                "ok",
                List.of()
        );

        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(18L)).thenReturn(Optional.of(converted));
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 18L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(livrableCatalogueService.findPublishedForPublicCatalogue(55L)).thenReturn(List.of(catalogueLivrable()));
        when(evaluationService.getLatestEvaluationByProjetCatalogue(55L)).thenReturn(evaluation);

        var response = industrialisationService.detailForCi(18L);

        assertEquals(55L, response.projetId());
        assertEquals(81.0, response.scoreEvaluationProjet());
        assertEquals(9L, response.latestEvaluation().id());
        assertTrue(response.eligibleIndustrialisation());
        assertEquals(1, response.livrables().size());
        assertEquals("Rapport", response.livrables().get(0).nom());
    }

    @Test
    void decideNoGo_catalogueCandidature_shouldNotRequireSujetProjet() {
        ProjetCatalogue catalogue = new ProjetCatalogue();
        catalogue.setId(55L);
        catalogue.setTitre("Catalogue IoT");
        catalogue.setStatut(StatutProjet.VALIDE);
        catalogue.setTypeProjet(TypeProjet.RDI);
        catalogue.setEncadrant(encadrant);
        catalogue.setDomaines(java.util.Set.of());
        catalogue.setTechnologies(java.util.Set.of());
        catalogue.setPrerequis(java.util.Set.of());

        CandidatureIndustrialisation converted = new CandidatureIndustrialisation();
        converted.setId(18L);
        converted.setProjetCatalogue(catalogue);
        converted.setTypeIndustrialisation(TypeIndustrialisation.INTERNE);
        converted.setStatut(StatutIndustrialisation.SOUMISE);
        converted.setDemandeur(encadrant);
        converted.setDateDemande(LocalDateTime.now());

        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(18L)).thenReturn(Optional.of(converted));
        when(candidatureRepository.save(converted)).thenReturn(converted);
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 18L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));
        when(livrableCatalogueService.findPublishedForPublicCatalogue(55L)).thenReturn(List.of());
        when(evaluationService.getLatestEvaluationByProjetCatalogue(55L))
                .thenThrow(new tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException("none"));

        var response = industrialisationService.decideNoGo(18L, new DecisionNoGoRequest("refus catalogue"));

        assertEquals(StatutIndustrialisation.NO_GO, response.statut());
        verify(notificationService).creerNotification(
                eq(7L), eq(NotificationType.CI_DEMANDE_REJETEE),
                eq("Demande industrialisation refusee"), anyString(),
                eq("/frontoffice/demandes-industrialisation"));
    }

    @Test
    void soumettre_catalogueCandidature_shouldUpdateProjetCatalogueStatus() {
        ProjetCatalogue catalogue = new ProjetCatalogue();
        catalogue.setId(55L);
        catalogue.setTitre("Catalogue IoT");
        catalogue.setStatut(StatutProjet.VALIDE);
        catalogue.setTypeProjet(TypeProjet.RDI);
        catalogue.setEncadrant(encadrant);
        catalogue.setDomaines(java.util.Set.of());
        catalogue.setTechnologies(java.util.Set.of());
        catalogue.setPrerequis(java.util.Set.of());

        CandidatureIndustrialisation converted = new CandidatureIndustrialisation();
        converted.setId(18L);
        converted.setProjetCatalogue(catalogue);
        converted.setSujetProjet(null);
        converted.setTypeIndustrialisation(TypeIndustrialisation.INTERNE);
        converted.setStatut(StatutIndustrialisation.BROUILLON);
        converted.setDemandeur(encadrant);
        converted.setDateDemande(LocalDateTime.now());

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(18L)).thenReturn(Optional.of(converted));
        when(livrableCatalogueService.findPublishedForPublicCatalogue(55L)).thenReturn(List.of(catalogueLivrable()));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(evaluationService.getLatestEvaluationByProjetCatalogue(55L))
                .thenThrow(new tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException("none"));
        when(projetCatalogueRepository.save(catalogue)).thenReturn(catalogue);
        when(candidatureRepository.save(converted)).thenReturn(converted);
        when(userRepository.findByRole(Role.ROLE_CI)).thenReturn(List.of(ci));
        when(historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, 18L, Pageable.unpaged()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        var response = industrialisationService.soumettre(18L, new SubmitIndustrialisationRequest(false));

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
        assertEquals(StatutProjet.CANDIDAT_INDUSTRIALISATION_INTERNE, catalogue.getStatut());
        verify(projetCatalogueRepository).save(catalogue);
        verify(notificationService).creerNotification(
                eq(9L), eq(NotificationType.CI_DEMANDE_SOUMISE),
                eq("Demande d'industrialisation"), anyString(),
                eq("/ci/industrialisation"));
    }

    private CataloguePublicLivrableResponse catalogueLivrable() {
        return new CataloguePublicLivrableResponse(
                99L,
                55L,
                "Catalogue IoT",
                tn.esprit.backend_esprittech.entity.livrable.TypeLivrable.RAPPORT,
                "Rapport",
                null,
                "rapport.pdf",
                "obj",
                "application/pdf",
                10L,
                null,
                7L,
                "Ali Mansour",
                LocalDateTime.now(),
                true,
                false
        );
    }
}
