package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationOverrideRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationValidationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlCriterionResult;
import tn.esprit.backend_esprittech.dto.CritereEval.MlDeliverableContent;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvidenceResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatNoteRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private CritereEliminatoireRepository critereEliminatoireRepository;
    @Mock private CritereNoteRepository critereNoteRepository;
    @Mock private EvaluationRepository evaluationRepository;
    @Mock private LivrableRepository livrableRepository;
    @Mock private NoteLevelService noteLevelService;
    @Mock private UserService userService;
    @Mock private DeliverableContentExtractionService contentExtractionService;
    @Mock private MlEvaluationClient mlEvaluationClient;

    @InjectMocks
    private EvaluationService evaluationService;

    private SujetProjet projet;
    private CritereEliminatoire eliminatoire;
    private CritereNote note;
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
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculerEvaluation(1L, new EvaluationRequest(
                List.of(new ResultatEliminatoireRequest(10L, ReponseEliminatoire.OK, "OK")),
                List.of(new ResultatNoteRequest(20L, 4.0, "Bon"))
        ));

        assertEquals(80.0, response.scoreFinal());
        assertEquals("MANUAL", response.mlStatus());
        assertEquals(TypeCritere.NOTE, response.resultats().get(1).typeCritere());
        assertEquals(4, response.resultats().get(1).noteValue());
        verify(sujetProjetRepository).save(projet);
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
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "Recalcul manuel");

        assertEquals(80.0, response.scoreFinal());
        assertEquals("COMPLETED", response.mlStatus());
        assertEquals("deliverable-content-evaluator-v1", response.mlModelVersion());
        assertEquals(0.8, response.mlGlobalConfidence());
        assertEquals(ModeEvaluation.CONTENT_ANALYSIS, response.resultats().get(0).modeEvaluation());
        assertNull(response.resultats().get(1).metadataUsed());
        assertEquals(0.8, response.resultats().get(1).normalizedScore());
        assertEquals(List.of(100L), response.resultats().get(1).evidenceLivrableIds());
        verify(contentExtractionService).extract(List.of(livrable));
        verify(mlEvaluationClient).evaluate(any());
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
        when(mlEvaluationClient.evaluate(any())).thenReturn(mlResponse(0.0, false));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "empty");

        assertEquals(0.0, response.scoreFinal());
        assertEquals(0, response.resultats().get(0).noteValue());
        assertEquals("Non evaluable", response.resultats().get(0).noteLabel());
        assertEquals(0.0, projet.getScoreFinalEvaluation());
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
                "COMPLETED", "deliverable-content-evaluator-v1", "deliverable-content-evaluator-v1",
                0.0, List.of(), List.of(), List.of(), List.of(), List.of()
        ));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        when(evaluationRepository.findBySujetProjetIdOrderByDateCalculDesc(1L)).thenReturn(List.of(evaluation));

        assertEquals(1, evaluationService.getEvaluationHistoryByProjet(1L).size());

        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(1L)).thenReturn(Optional.of(evaluation));
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
    }

    @Test
    void getLatestEvaluation_studentShouldBeDenied() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class, () -> evaluationService.getLatestEvaluationByProjet(1L));
    }

    private MlEvaluationResponse mlResponse(double noteNormalized, boolean eliminatorySatisfied) {
        MlEvidenceResponse evidence = new MlEvidenceResponse(100L, "Architecture API et tests", 0.8, List.of("architecture", "test"));
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
                "deliverable-content-evaluator-v1",
                "deliverable-content-evaluator-v1",
                0.8,
                List.of("Contenu exploitable"),
                List.of(),
                List.of("Completer les tests"),
                List.of(),
                List.of(eliminatoryResult, noteResult)
        );
    }

    private Evaluation savedEvaluation() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(82.0);
        evaluation.setMlScore(82.0);
        evaluation.setMlStatus("COMPLETED");
        evaluation.setMlModelVersion("deliverable-content-evaluator-v1");
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
