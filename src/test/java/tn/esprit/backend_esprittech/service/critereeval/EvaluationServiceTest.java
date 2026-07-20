package tn.esprit.backend_esprittech.service.critereeval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationOverrideRequest;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationValidationRequest;
import tn.esprit.backend_esprittech.dto.critereeval.MlCriterionResult;
import tn.esprit.backend_esprittech.dto.critereeval.MlDeliverableContent;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvaluationResponse;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvidenceResponse;
import tn.esprit.backend_esprittech.dto.critereeval.ResultatEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.critereeval.ResultatNoteRequest;
import tn.esprit.backend_esprittech.entity.critereeval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNote;
import tn.esprit.backend_esprittech.entity.critereeval.Evaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.EvaluationPersistenceException;
import tn.esprit.backend_esprittech.exception.critereeval.BadRequestException;
import tn.esprit.backend_esprittech.repository.critereeval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.critereeval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;
import tn.esprit.backend_esprittech.service.projetcatalogue.ProjetCatalogueService;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private CritereEliminatoireRepository critereEliminatoireRepository;
    @Mock private CritereNoteRepository critereNoteRepository;
    @Mock private EvaluationRepository evaluationRepository;
    @Mock private LivrableRepository livrableRepository;
    @Mock private LivrableCatalogueRepository livrableCatalogueRepository;
    @Mock private NoteLevelService noteLevelService;
    @Mock private UserService userService;
    @Mock private DeliverableContentExtractionService contentExtractionService;
    @Mock private MlEvaluationClient mlEvaluationClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private ProjetCatalogueService projetCatalogueService;
    @Mock private NotificationService notificationService;
    @Mock private ProjetCatalogueRepository projetCatalogueRepository;

    @InjectMocks
    private EvaluationService evaluationService;

    private SujetProjet projet;
    private CritereEliminatoire eliminatoire;
    private CritereNote note;
    private ProjetCatalogue catalogueProjet;
    private LivrableCatalogue catalogueGitLivrable;
    private User admin;
    private User ci;
    private User encadrant;
    private User etudiant;

    @BeforeEach
    void setUp() {
        projet = new SujetProjet();
        projet.setId(1L);
        projet.setTitre("Plateforme IoT");
        projet.setDomaine("Industrialisation");
        projet.setTechnologies("Spring Boot, Angular, Docker");
        projet.setEncadrantId(11L);
        projet.setStatut(StatutSujet.REALISATION_TERMINEE);

        eliminatoire = new CritereEliminatoire();
        eliminatoire.setId(10L);
        eliminatoire.setLibelle("Documentation presente");
        eliminatoire.setDomaine("Documentation");
        eliminatoire.setDescription("Guide installation et usage");
        eliminatoire.setActif(true);
        eliminatoire.setOrdre(1);

        note = new CritereNote();
        note.setId(20L);
        note.setLibelle("Qualite technique");
        note.setDomaine("Technique");
        note.setDescription("Architecture, securite, tests et deploiement");
        note.setActif(true);
        note.setOrdre(1);
        note.setBareme(5.0);
        note.setPoids(2.0);
        note.setSeuil(3.0);
        note.setDefaultNoteValue(3);

        catalogueProjet = new ProjetCatalogue();
        catalogueProjet.setId(7L);
        catalogueProjet.setTitre("Projet catalogue IA");
        catalogueProjet.setTypeProjet(TypeProjet.RDI);
        catalogueProjet.setStatut(StatutProjet.VALIDE);
        catalogueProjet.setScore(0);
        catalogueProjet.setDomaines(new LinkedHashSet<>());
        catalogueProjet.setTechnologies(new LinkedHashSet<>());
        catalogueProjet.setPrerequis(new LinkedHashSet<>());

        catalogueGitLivrable = new LivrableCatalogue();
        catalogueGitLivrable.setId(700L);
        catalogueGitLivrable.setProjet(catalogueProjet);
        catalogueGitLivrable.setTypeLivrable(TypeLivrable.LIEN_GIT);
        catalogueGitLivrable.setNom("Depot Git");
        catalogueGitLivrable.setLienExterne("https://github.com/esprit/projet-catalogue.git");
        catalogueGitLivrable.setActif(true);

        admin = user(99L, Role.ROLE_ADMIN);
        ci = user(77L, Role.ROLE_CI);
        encadrant = user(11L, Role.ROLE_ENSEIGNANT);
        etudiant = user(88L, Role.ROLE_ETUDIANT);

        lenient().when(noteLevelService.labelFor(any())).thenAnswer(invocation -> {
            Integer value = invocation.getArgument(0);
            return value == null ? null : "Niveau " + value;
        });
    }

    @Test
    void calculerEvaluation_manualResults_shouldPersistWeightedScore() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(eliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(evaluationRepository.saveAndFlush(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculerEvaluation(1L, new EvaluationRequest(
                List.of(new ResultatEliminatoireRequest(10L, ReponseEliminatoire.OK, "OK")),
                List.of(new ResultatNoteRequest(20L, 4.0, "Bon"))
        ));

        assertEquals(80.0, response.scoreFinal());
        assertEquals("MANUAL", response.mlStatus());
        assertEquals(TypeCritere.NOTE, response.resultats().get(1).typeCritere());
        assertEquals(4, response.resultats().get(1).noteValue());
        verify(sujetProjetRepository).save(projet);
        verify(notificationService).creerNotification(
                eq(11L), eq(NotificationType.EVALUATION_CALCULEE),
                eq("Evaluation calculee"), anyString(), eq("/frontoffice/mes-projets"));
    }

    @Test
    void calculateAutomaticEvaluation_shouldUseExtractedContentAndMlScoresOnly() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT);
        MlDeliverableContent content = new MlDeliverableContent(
                livrable.getId(), "RAPPORT", "Rapport", "text/plain", "READY", null,
                "Architecture API, securite JWT, tests et guide installation Docker."
        );

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(eliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(contentExtractionService.extract(List.of(livrable))).thenReturn(List.of(content));
        when(mlEvaluationClient.evaluate(any())).thenReturn(mlResponse(0.8, true));
        ArgumentCaptor<Evaluation> evaluationCaptor = ArgumentCaptor.forClass(Evaluation.class);
        when(evaluationRepository.saveAndFlush(evaluationCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "Recalcul manuel");

        assertEquals(80.0, response.scoreFinal());
        assertEquals(1L, response.sujetProjetId());
        assertNull(response.projetCatalogueId());
        assertEquals("SUJET_PROJET", response.evaluationContext());
        assertEquals("COMPLETED", response.mlStatus());
        assertEquals("2.0.0", response.mlModelVersion());
        assertEquals("multimodal-semantic-v2", response.pipelineVersion());
        assertEquals(0.8, response.mlGlobalConfidence());
        assertEquals(ModeEvaluation.CONTENT_ANALYSIS, response.resultats().get(0).modeEvaluation());
        assertNull(response.resultats().get(1).metadataUsed());
        assertEquals(0.8, response.resultats().get(1).normalizedScore());
        assertEquals(List.of(100L), response.resultats().get(1).evidenceLivrableIds());
        Evaluation saved = evaluationCaptor.getValue();
        assertSame(projet, saved.getSujetProjet());
        assertNull(saved.getProjetCatalogue());
        verify(contentExtractionService).extract(List.of(livrable));
        verify(mlEvaluationClient).evaluate(any());
        verify(notificationService).creerNotification(
                eq(11L), eq(NotificationType.EVALUATION_CALCULEE),
                eq("Evaluation calculee"), anyString(), eq("/frontoffice/mes-projets"));
    }

    @Test
    void calculateAutomaticEvaluationForCatalogue_shouldPersistCatalogueParentOnly() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(projetCatalogueRepository.findById(7L)).thenReturn(Optional.of(catalogueProjet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(eliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableCatalogueRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(7L))
                .thenReturn(List.of(catalogueGitLivrable));
        MlDeliverableContent catalogueContent = new MlDeliverableContent(
                catalogueGitLivrable.getId(), "LIEN_GIT", "Depot Git", "text/plain", "READY", null, "lien git content");
        when(contentExtractionService.extractCatalogue(List.of(catalogueGitLivrable))).thenReturn(List.of(catalogueContent));
        MlEvidenceResponse catalogueEvidence = new MlEvidenceResponse(catalogueGitLivrable.getId(), "Architecture API et tests", 0.8, List.of("architecture", "test"));
        MlCriterionResult catalogueElimResult = new MlCriterionResult(
                10L, "ELIMINATOIRE", 1.0, 1.0, 0.8, 0.8, true,
                "Documentation analysee", List.of(catalogueEvidence), List.of("Doc claire"), List.of(), List.of("Maintenir")
        );
        MlCriterionResult catalogueNoteResult = new MlCriterionResult(
                20L, "NOTE", 4.5, 5.0, 0.9, 0.8, null,
                "Qualite analysee", List.of(catalogueEvidence), List.of("Architecture presente"), List.of(), List.of("Completer les tests")
        );
        MlEvaluationResponse catalogueMlResponse = new MlEvaluationResponse(
                "COMPLETED", "2.0.0", "multimodal-semantic-v2", 0.8,
                List.of("Contenu exploitable"), List.of(), List.of("Completer les tests"), List.of(),
                List.of(catalogueElimResult, catalogueNoteResult)
        );
        when(mlEvaluationClient.evaluate(any())).thenReturn(catalogueMlResponse);
        ArgumentCaptor<Evaluation> evaluationCaptor = ArgumentCaptor.forClass(Evaluation.class);
        when(evaluationRepository.saveAndFlush(evaluationCaptor.capture())).thenAnswer(invocation -> {
            Evaluation evaluation = invocation.getArgument(0);
            evaluation.setId(70L);
            evaluation.setDateCalcul(LocalDateTime.now());
            return evaluation;
        });
        when(projetCatalogueRepository.save(catalogueProjet)).thenReturn(catalogueProjet);

        var response = evaluationService.calculateAutomaticEvaluationForCatalogue(7L, "Recalcul catalogue");

        Evaluation saved = evaluationCaptor.getValue();
        assertSame(catalogueProjet, saved.getProjetCatalogue());
        assertNull(saved.getSujetProjet());
        assertEquals(7L, response.projetCatalogueId());
        assertNull(response.sujetProjetId());
        assertEquals("PROJET_CATALOGUE", response.evaluationContext());
        assertEquals(90.0, response.scoreFinal());
        assertEquals("COMPLETED", response.mlStatus());
        assertEquals("multimodal-semantic-v2", response.pipelineVersion());
        assertEquals(2, saved.getResultats().size());
        assertEquals(90, catalogueProjet.getScore());
        verify(contentExtractionService).extractCatalogue(List.of(catalogueGitLivrable));
        verify(mlEvaluationClient).evaluate(any());
        verify(projetCatalogueRepository).save(catalogueProjet);
        verify(sujetProjetRepository, never()).save(any());
    }

    @Test
    void validateEvaluationParent_withoutParent_shouldRejectExplicitly() {
        Evaluation evaluation = new Evaluation();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.validateEvaluationParent(evaluation));

        assertEquals("Une evaluation doit etre rattachee exactement a un SujetProjet ou a un ProjetCatalogue.",
                ex.getMessage());
    }

    @Test
    void validateEvaluationParent_withTwoParents_shouldRejectExplicitly() {
        Evaluation evaluation = new Evaluation();
        evaluation.setSujetProjet(projet);
        evaluation.setProjetCatalogue(catalogueProjet);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.validateEvaluationParent(evaluation));

        assertEquals("Une evaluation doit etre rattachee exactement a un SujetProjet ou a un ProjetCatalogue.",
                ex.getMessage());
    }

    @Test
    void getCatalogueEvaluations_shouldUseCatalogueIdAndStableOrdering() {
        Evaluation older = catalogueEvaluation(80L, LocalDateTime.of(2026, 7, 16, 10, 0), 70.0);
        Evaluation latest = catalogueEvaluation(81L, LocalDateTime.of(2026, 7, 16, 10, 0), 82.0);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(projetCatalogueRepository.findById(7L)).thenReturn(Optional.of(catalogueProjet));
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(7L))
                .thenReturn(Optional.of(latest));
        when(evaluationRepository.findByProjetCatalogueIdOrderByDateCalculDescIdDesc(7L))
                .thenReturn(List.of(latest, older));

        var latestResponse = evaluationService.getLatestEvaluationByProjetCatalogue(7L);
        var history = evaluationService.getEvaluationHistoryByProjetCatalogue(7L);

        assertEquals(81L, latestResponse.id());
        assertNull(latestResponse.sujetProjetId());
        assertEquals(7L, latestResponse.projetCatalogueId());
        assertEquals(List.of(81L, 80L), history.stream().map(response -> response.id()).toList());
        verify(evaluationRepository).findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(7L);
        verify(evaluationRepository).findByProjetCatalogueIdOrderByDateCalculDescIdDesc(7L);
    }

    @Test
    void getLatestEvaluationByProjetCatalogue_afterLeavingValide_shouldKeepUnchangedEvaluation() {
        Evaluation latest = catalogueEvaluation(81L, LocalDateTime.of(2026, 7, 16, 10, 0), 82.0);
        ResultatCritere detail = new ResultatCritere();
        detail.setCritereId(20L);
        detail.setCritereLibelle("Qualite technique");
        detail.setTypeCritere(TypeCritere.NOTE);
        detail.setRuleConfigured(true);
        detail.setNoteValue(4);
        detail.setPoids(2.0);
        detail.setEvidenceSummary("Evidence ML");
        latest.addResultat(detail);
        catalogueProjet.setScore(82);

        when(userService.getCurrentUser()).thenReturn(admin);
        when(projetCatalogueRepository.findById(7L)).thenReturn(Optional.of(catalogueProjet));
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(7L))
                .thenReturn(Optional.of(latest));
        when(evaluationRepository.findByProjetCatalogueIdOrderByDateCalculDescIdDesc(7L))
                .thenReturn(List.of(latest));

        double scoreBefore = latest.getScoreFinal();
        int resultCountBefore = latest.getResultats().size();
        int historyCountBefore = evaluationService.getEvaluationHistoryByProjetCatalogue(7L).size();

        for (StatutProjet statut : List.of(
                StatutProjet.CANDIDAT_INDUSTRIALISATION_INTERNE,
                StatutProjet.INDUSTRIALISE_DSI
        )) {
            catalogueProjet.setStatut(statut);

            var response = evaluationService.getLatestEvaluationByProjetCatalogue(7L);
            var history = evaluationService.getEvaluationHistoryByProjetCatalogue(7L);

            assertEquals(81L, response.id());
            assertEquals(scoreBefore, response.scoreFinal());
            assertEquals(resultCountBefore, response.resultats().size());
            assertEquals("Qualite technique", response.resultats().get(0).critereLibelle());
            assertEquals(historyCountBefore, history.size());
            assertEquals(82, catalogueProjet.getScore());
        }

        verify(evaluationRepository, never()).delete(any());
        verify(evaluationRepository, never()).deleteById(any());
        verify(evaluationRepository, never()).save(any());
        verify(evaluationRepository, never()).saveAndFlush(any());
    }

    @Test
    void getLatestEvaluationForPublishedCatalogue_afterLeavingValide_shouldStillReturnEvaluation() {
        Evaluation latest = catalogueEvaluation(81L, LocalDateTime.of(2026, 7, 16, 10, 0), 82.0);
        catalogueProjet.setStatut(StatutProjet.CANDIDAT_INDUSTRIALISATION_EXTERNE);
        catalogueProjet.setScore(82);
        when(projetCatalogueRepository.findById(7L)).thenReturn(Optional.of(catalogueProjet));
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(7L))
                .thenReturn(Optional.of(latest));

        var response = evaluationService.getLatestEvaluationForPublishedCatalogue(7L);

        assertEquals(82.0, response.scoreFinal());
        assertEquals(81L, response.id());
        verify(evaluationRepository, never()).save(any());
        verify(evaluationRepository, never()).delete(any());
    }

    @Test
    void calculateAutomaticEvaluationForCatalogue_whenPersistenceFails_shouldNotSaveProjectScore() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(projetCatalogueRepository.findById(7L)).thenReturn(Optional.of(catalogueProjet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableCatalogueRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(7L))
                .thenReturn(List.of(catalogueGitLivrable));
        when(contentExtractionService.extractCatalogue(List.of(catalogueGitLivrable))).thenReturn(List.of());
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "COMPLETED", "2.0.0", "multimodal-semantic-v2",
                0.8, List.of(), List.of(), List.of(), List.of(),
                List.of(new MlCriterionResult(
                        20L, "NOTE", 4.0, 5.0, 0.8, 0.8, null,
                        "Qualite analysee", List.of(), List.of(), List.of(), List.of()
                ))
        ));
        doThrow(new RuntimeException("db down")).when(evaluationRepository).saveAndFlush(any(Evaluation.class));

        assertThrows(EvaluationPersistenceException.class,
                () -> evaluationService.calculateAutomaticEvaluationForCatalogue(7L, "rollback"));

        verify(projetCatalogueRepository, never()).save(any());
        verifyNoInteractions(projetCatalogueService);
    }

    @Test
    void calculateAutomaticEvaluation_emptyContentFromMl_shouldScoreZeroDespiteLivrableMetadata() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT);
        livrable.setOriginalFileName("rapport-final.pdf");
        livrable.setSize(2_000_000L);

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(contentExtractionService.extract(List.of(livrable))).thenReturn(List.of(new MlDeliverableContent(
                livrable.getId(), "RAPPORT", "Rapport", "application/pdf", "UNSUPPORTED",
                "No readable text", null
        )));
        MlCriterionResult noteResult = new MlCriterionResult(
                20L,
                "NOTE",
                null,
                5.0,
                null,
                0.8,
                null,
                "Qualite analysee",
                List.of(),
                List.of(),
                List.of("INSUFFICIENT_EVIDENCE"),
                List.of("ADD_TRACEABLE_EVIDENCE")
        );
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "COMPLETED",
                "2.0.0",
                "multimodal-semantic-v2",
                0.8,
                List.of("Contenu exploitable"),
                List.of(),
                List.of("Completer les tests"),
                List.of(),
                List.of(noteResult)
        ));
        when(evaluationRepository.saveAndFlush(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "empty");

        assertNull(response.scoreFinal());
        assertNull(response.resultats().get(0).noteValue());
        assertEquals("Non calculé", response.resultats().get(0).noteLabel());
        assertNull(projet.getScoreFinalEvaluation());
    }

    @Test
    void calculateAutomaticEvaluation_insufficientEvidence_shouldPersistNullMlScoreAndZeroContribution() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT);
        MlDeliverableContent content = new MlDeliverableContent(
                livrable.getId(), "RAPPORT", "Rapport", "text/plain", "READY", null,
                "Contenu sans preuve pertinente."
        );
        MlCriterionResult insufficient = new MlCriterionResult(
                20L,
                "NOTE",
                null,
                5.0,
                null,
                0.2,
                null,
                "Aucune preuve pertinente.",
                List.of(),
                List.of(),
                List.of("INSUFFICIENT_EVIDENCE"),
                List.of("ADD_TRACEABLE_EVIDENCE")
        );

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(contentExtractionService.extract(List.of(livrable))).thenReturn(List.of(content));
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "INSUFFICIENT_EVIDENCE", "2.0.0", "multimodal-semantic-v2",
                0.2, List.of(), List.of("INSUFFICIENT_EVIDENCE"),
                List.of("ADD_TRACEABLE_EVIDENCE"), List.of(), List.of(insufficient)
        ));
        when(evaluationRepository.saveAndFlush(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "insufficient");

        assertNull(response.scoreFinal());
        assertNull(response.resultats().get(0).normalizedScore());
        assertEquals("INSUFFICIENT_EVIDENCE", response.resultats().get(0).criterionStatus());
        assertNull(response.resultats().get(0).noteValue());
        assertNull(response.resultats().get(0).noteObtenue());
        assertNull(response.resultats().get(0).scorePondere());
        assertEquals("Non calculé", response.resultats().get(0).noteLabel());
    }

    @Test
    void calculateAutomaticEvaluation_duplicateCriterionResult_shouldRejectResponse() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT);
        MlCriterionResult duplicate = new MlCriterionResult(
                20L, "NOTE", 4.0, 5.0, 0.8, 0.8, null,
                "Qualite analysee", List.of(), List.of(), List.of(), List.of()
        );

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(contentExtractionService.extract(List.of(livrable))).thenReturn(List.of(new MlDeliverableContent(
                livrable.getId(), "RAPPORT", "Rapport", "text/plain", "READY", null,
                "Architecture API."
        )));
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "COMPLETED", "2.0.0", "multimodal-semantic-v2",
                0.8, List.of(), List.of(), List.of(), List.of(), List.of(duplicate, duplicate)
        ));

        assertThrows(BusinessException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "duplicate"));
        verify(evaluationRepository, never()).save(any(Evaluation.class));
    }

    @Test
    void calculateAutomaticEvaluation_invalidNormalizedScore_shouldRejectResponse() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT);
        MlCriterionResult invalid = new MlCriterionResult(
                20L, "NOTE", 6.0, 5.0, 1.2, 0.8, null,
                "Score invalide", List.of(), List.of(), List.of(), List.of()
        );

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(contentExtractionService.extract(List.of(livrable))).thenReturn(List.of(new MlDeliverableContent(
                livrable.getId(), "RAPPORT", "Rapport", "text/plain", "READY", null,
                "Architecture API."
        )));
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "COMPLETED", "2.0.0", "multimodal-semantic-v2",
                0.8, List.of(), List.of(), List.of(), List.of(), List.of(invalid)
        ));

        assertThrows(BusinessException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "invalid"));
        verify(evaluationRepository, never()).save(any(Evaluation.class));
    }

    @Test
    void calculateAutomaticEvaluation_semanticSupportsUnknownProjectRelevance_shouldRejectResponse() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT);
        MlEvidenceResponse evidence = mlEvidence("SUPPORTS", "UNKNOWN", "SEMANTIC");
        MlCriterionResult invalid = new MlCriterionResult(
                20L, "NOTE", 4.0, 5.0, 0.8, 0.8, null,
                "EVIDENCE_CONDITIONED_SCORE", List.of(evidence), List.of(), List.of(), List.of()
        );

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(contentExtractionService.extract(List.of(livrable))).thenReturn(List.of(new MlDeliverableContent(
                livrable.getId(), "RAPPORT", "Rapport", "text/plain", "READY", null,
                "Architecture API."
        )));
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "COMPLETED", "2.0.0", "multimodal-semantic-v2",
                0.8, List.of(), List.of(), List.of(), List.of(), List.of(invalid)
        ));

        assertThrows(BusinessException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "invalid evidence"));
        verify(evaluationRepository, never()).save(any(Evaluation.class));
    }

    @Test
    void calculateAutomaticEvaluation_deterministicSupportsUnknownProjectRelevance_shouldBeAccepted() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT);
        MlEvidenceResponse evidence = mlEvidence("SUPPORTS", "UNKNOWN", "DETERMINISTIC");
        MlCriterionResult valid = new MlCriterionResult(
                20L, "NOTE", 4.0, 5.0, 0.8, 0.8, null,
                "DETERMINISTIC_SOURCE_SIGNAL_CONFIRMED", List.of(evidence), List.of(), List.of(), List.of()
        );

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(note));
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(contentExtractionService.extract(List.of(livrable))).thenReturn(List.of(new MlDeliverableContent(
                livrable.getId(), "RAPPORT", "Rapport", "text/plain", "READY", null,
                "Execution report."
        )));
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "COMPLETED", "2.0.0", "multimodal-semantic-v2",
                0.8, List.of(), List.of(), List.of(), List.of(), List.of(valid)
        ));
        when(evaluationRepository.saveAndFlush(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "deterministic evidence");

        assertEquals(80.0, response.scoreFinal());
        assertEquals("COMPLETED", response.mlStatus());
        verify(notificationService).creerNotification(
                eq(11L), eq(NotificationType.EVALUATION_CALCULEE),
                eq("Evaluation calculee"), anyString(), eq("/frontoffice/mes-projets"));
    }

    @Test
    void calculateAutomaticEvaluation_encadrantCanRecalculateOwnProjectOnly() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of());
        when(contentExtractionService.extract(List.of())).thenReturn(List.of());
        when(mlEvaluationClient.evaluate(any())).thenReturn(new MlEvaluationResponse(
                "COMPLETED", "2.0.0", "multimodal-semantic-v2",
                0.0, List.of(), List.of(), List.of(), List.of(), List.of()
        ));
        when(evaluationRepository.saveAndFlush(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("ROLE_ENSEIGNANT User11 Test", evaluationService
                .calculateAutomaticEvaluation(1L, "Nouveaux livrables").calculatedBy());

        projet.setEncadrantId(55L);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "Nouveaux livrables"));
    }

    @Test
    void calculateAutomaticEvaluation_studentAndNonTerminatedProjectShouldFail() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        assertThrows(AccessDeniedException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "Recalcul"));

        projet.setStatut(StatutSujet.REALISATION_EN_COURS);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        assertThrows(BadRequestException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "Recalcul"));
    }

    @Test
    void getHistoryValidateRejectAndOverride_shouldPersistManualDecisionState() {
        Evaluation evaluation = savedEvaluation();
        when(userService.getCurrentUser()).thenReturn(ci);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(evaluationRepository.findBySujetProjetIdOrderByDateCalculDescIdDesc(1L)).thenReturn(List.of(evaluation));

        assertEquals(1, evaluationService.getEvaluationHistoryByProjet(1L).size());

        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(1L)).thenReturn(Optional.of(evaluation));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var validated = evaluationService.validateLatestEvaluation(1L, new EvaluationValidationRequest("Valide"));
        assertEquals("VALIDATED", validated.validationStatus());
        assertEquals(82.0, validated.finalValidatedScore());
        assertEquals("ROLE_CI User77 Test", validated.validatedBy());

        var rejected = evaluationService.rejectLatestEvaluation(1L, new EvaluationValidationRequest("A revoir"));
        assertEquals("REJECTED", rejected.validationStatus());

        when(userService.getCurrentUser()).thenReturn(admin);
        var overridden = evaluationService.overrideLatestEvaluation(1L, new EvaluationOverrideRequest(91.5, "Avis admin"));
        assertEquals("OVERRIDDEN", overridden.validationStatus());
        assertEquals(91.5, overridden.finalValidatedScore());
        assertEquals("Avis admin", overridden.overrideReason());
        assertEquals(91.5, projet.getScoreFinalEvaluation());

        verify(notificationService).creerNotification(
                eq(11L), eq(NotificationType.EVALUATION_TERMINEE),
                eq("Evaluation terminee"), anyString(), eq("/frontoffice/mes-projets"));
        verify(notificationService).creerNotification(
                eq(11L), eq(NotificationType.EVALUATION_TERMINEE),
                eq("Evaluation refusee"), anyString(), eq("/frontoffice/mes-projets"));
        verify(notificationService).creerNotification(
                eq(11L), eq(NotificationType.EVALUATION_TERMINEE),
                eq("Evaluation surchargee"), anyString(), eq("/frontoffice/mes-projets"));
    }

    @Test
    void getLatestEvaluation_studentShouldBeDenied() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class, () -> evaluationService.getLatestEvaluationByProjet(1L));
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsEvaluationFields() {
        Evaluation evaluation = savedEvaluation();
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(1L))
                .thenReturn(Optional.of(evaluation));

        var result = evaluationService.getOldValues(1L);

        assertEquals(evaluation.getValidationStatus(), result.get("validationStatus"));
        assertEquals(evaluation.getScoreFinal(), result.get("scoreFinal"));
        assertEquals(evaluation.getFinalValidatedScore(), result.get("finalValidatedScore"));
    }

    @Test
    void getOldValues_noEvaluation_returnsEmptyMap() {
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(1L))
                .thenReturn(Optional.empty());

        var result = evaluationService.getOldValues(1L);

        assertTrue(result.isEmpty());
    }

    private MlEvaluationResponse mlResponse(double noteNormalized, boolean eliminatorySatisfied) {
        MlEvidenceResponse evidence = eliminatorySatisfied
                ? new MlEvidenceResponse(100L, "Architecture API et tests", 0.8, List.of("architecture", "test"))
                : new MlEvidenceResponse(100L, "Preuve negative confirmee", 0.8, "CONTRADICTS", 0.8);
        MlCriterionResult eliminatoryResult = new MlCriterionResult(
                10L, "ELIMINATOIRE", eliminatorySatisfied ? 1.0 : 0.0, 1.0,
                eliminatorySatisfied ? 0.8 : 0.0, 0.8, eliminatorySatisfied,
                "Documentation analysee", List.of(evidence), List.of("Doc claire"), List.of(), List.of("Maintenir la documentation")
        );
        MlCriterionResult noteResult = new MlCriterionResult(
                20L, "NOTE", noteNormalized * 5.0, 5.0, noteNormalized, 0.8, null,
                "Qualite analysee", List.of(evidence), List.of("Architecture presente"), List.of(), List.of("Completer les tests")
        );
        return new MlEvaluationResponse(
                "COMPLETED",
                "2.0.0",
                "multimodal-semantic-v2",
                0.8,
                List.of("Contenu exploitable"),
                List.of(),
                List.of("Completer les tests"),
                List.of(),
                List.of(eliminatoryResult, noteResult)
        );
    }

    private MlEvidenceResponse mlEvidence(String stance, String projectRelevanceType, String evidenceNature) {
        return new MlEvidenceResponse(
                "ev-test",
                100L,
                "TXT",
                "rapport.txt",
                null,
                null,
                null,
                null,
                "hash",
                "TEXT_CONTENT",
                "Preuve projet tracee.",
                0.8,
                0.8,
                0.8,
                0.4,
                0.2,
                stance,
                projectRelevanceType,
                0.8,
                0.8,
                0.0,
                0.8,
                0.9,
                "PROJECT_DELIVERABLE",
                0.9,
                List.of("TEST_FIXTURE"),
                evidenceNature,
                List.of()
        );
    }

    private Evaluation savedEvaluation() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(82.0);
        evaluation.setMlScore(82.0);
        evaluation.setMlStatus("COMPLETED");
        evaluation.setMlModelVersion("2.0.0");
        evaluation.setPipelineVersion("multimodal-semantic-v2");
        evaluation.setEngineType("multimodal-semantic-v2");
        evaluation.setMlGlobalConfidence(0.74);
        evaluation.setEligibleIndustrialisation(true);
        evaluation.setBloqueParEliminatoire(false);
        evaluation.setHasEliminatoryWarnings(false);
        evaluation.setEliminatoryWarningsCount(0);
        evaluation.setDateCalcul(LocalDateTime.now());
        evaluation.setValidationStatus("PENDING");

        ResultatCritere resultat = new ResultatCritere();
        resultat.setId(40L);
        resultat.setCritereId(20L);
        resultat.setCritereLibelle("Qualite technique");
        resultat.setTypeCritere(TypeCritere.NOTE);
        resultat.setModeEvaluation(ModeEvaluation.CONTENT_ANALYSIS);
        resultat.setRuleConfigured(true);
        resultat.setNoteValue(4);
        resultat.setNoteLabel("Niveau 4");
        resultat.setNoteObtenue(4.0);
        resultat.setBareme(5.0);
        resultat.setPoids(2.0);
        resultat.setScorePondere(1.6);
        evaluation.addResultat(resultat);
        return evaluation;
    }

    private Evaluation catalogueEvaluation(Long id, LocalDateTime dateCalcul, double scoreFinal) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(id);
        evaluation.setSujetProjet(null);
        evaluation.setProjetCatalogue(catalogueProjet);
        evaluation.setScoreFinal(scoreFinal);
        evaluation.setMlScore(scoreFinal);
        evaluation.setMlStatus("COMPLETED");
        evaluation.setMlModelVersion("2.0.0");
        evaluation.setPipelineVersion("multimodal-semantic-v2");
        evaluation.setEngineType("multimodal-semantic-v2");
        evaluation.setMlGlobalConfidence(0.8);
        evaluation.setEligibleIndustrialisation(true);
        evaluation.setBloqueParEliminatoire(false);
        evaluation.setHasEliminatoryWarnings(false);
        evaluation.setEliminatoryWarningsCount(0);
        evaluation.setDateCalcul(dateCalcul);
        evaluation.setValidationStatus("PENDING");
        return evaluation;
    }

    private Livrable livrable(TypeLivrable typeLivrable) {
        Livrable livrable = new Livrable();
        livrable.setId(100L);
        livrable.setTypeLivrable(typeLivrable);
        livrable.setNom("Rapport");
        livrable.setActif(true);
        livrable.setObjectName("livrables/1/rapport.txt");
        return livrable;
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setPrenom("User" + id);
        user.setNom("Test");
        user.setRole(role);
        return user;
    }
}
