package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatNoteRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNoteRule;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.RuleOperator;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private SujetProjetRepository sujetProjetRepository;

    @Mock
    private CritereEliminatoireRepository critereEliminatoireRepository;

    @Mock
    private CritereNoteRepository critereNoteRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private LivrableRepository livrableRepository;

    @Mock
    private CritereNoteRuleRepository critereNoteRuleRepository;

    @Mock
    private NoteLevelService noteLevelService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EvaluationService evaluationService;

    private SujetProjet projet;
    private CritereEliminatoire critereEliminatoire;
    private CritereNote critereNote;
    private User admin;
    private User encadrant;
    private User etudiant;
    private Map<Long, CritereNote> notesById;

    @BeforeEach
    void setUp() {
        projet = new SujetProjet();
        projet.setId(1L);
        projet.setTitre("Plateforme IoT");
        projet.setEncadrantId(11L);
        projet.setStatut(StatutSujet.REALISATION_TERMINEE);

        critereEliminatoire = new CritereEliminatoire();
        critereEliminatoire.setId(1L);
        critereEliminatoire.setLibelle("Documentation");
        critereEliminatoire.setDomaine("documentation");
        critereEliminatoire.setActif(true);
        critereEliminatoire.setOrdre(1);
        critereEliminatoire.setRuleEnabled(true);
        critereEliminatoire.setModeEvaluation(ModeEvaluation.DOCUMENTATION_PRESENT);
        critereEliminatoire.setExpectedLivrableTypes("DOCUMENTATION,RAPPORT");

        critereNote = new CritereNote();
        critereNote.setId(2L);
        critereNote.setLibelle("Rapport");
        critereNote.setDomaine("rapport");
        critereNote.setActif(true);
        critereNote.setOrdre(1);
        critereNote.setBareme(5.0);
        critereNote.setPoids(2.0);
        critereNote.setSeuil(1.0);
        critereNote.setRuleEnabled(true);
        critereNote.setModeEvaluation(ModeEvaluation.LIVRABLE_TYPE_PRESENT);
        critereNote.setExpectedLivrableTypes("RAPPORT");
        critereNote.setNoteMaxAuto(5.0);
        critereNote.setDefaultNoteValue(3);

        notesById = new HashMap<>();
        notesById.put(critereNote.getId(), critereNote);
        lenient().when(noteLevelService.labelFor(anyInt())).thenAnswer(invocation -> switch (invocation.getArgument(0, Integer.class)) {
            case 1 -> "Très insatisfait";
            case 2 -> "Insatisfait";
            case 3 -> "Peu satisfait";
            case 4 -> "Satisfait";
            case 5 -> "Très satisfait";
            default -> "Niveau";
        });
        lenient().when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5));
        lenient().when(critereNoteRuleRepository.findByCritereNoteIdAndActiveTrueOrderByPriorityAscIdAsc(anyLong()))
                .thenAnswer(invocation -> legacyRulesFor(notesById.get(invocation.getArgument(0, Long.class))));

        admin = new User();
        admin.setId(99L);
        admin.setPrenom("Admin");
        admin.setNom("Root");
        admin.setRole(Role.ROLE_ADMIN);

        encadrant = new User();
        encadrant.setId(11L);
        encadrant.setPrenom("Sami");
        encadrant.setNom("Ben Ali");
        encadrant.setRole(Role.ROLE_ENSEIGNANT);

        etudiant = new User();
        etudiant.setId(88L);
        etudiant.setPrenom("Etu");
        etudiant.setNom("Diant");
        etudiant.setRole(Role.ROLE_ETUDIANT);
    }

    @Test
    void calculerEvaluation_allCriteriaOk_shouldReturnEligibleAndScoreComputed() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationRequest request = new EvaluationRequest(
                List.of(new ResultatEliminatoireRequest(1L, ReponseEliminatoire.OK, "OK commentaire")),
                List.of(new ResultatNoteRequest(2L, 4.0, "Note commentaire"))
        );

        var response = evaluationService.calculerEvaluation(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.sujetProjetId());
        assertEquals(80.0, response.scoreFinal());
        assertTrue(response.eligibleIndustrialisation());
        assertFalse(response.bloqueParEliminatoire());
        assertEquals(TypeCritere.ELIMINATOIRE, response.resultats().get(0).typeCritere());
        assertEquals(TypeCritere.NOTE, response.resultats().get(1).typeCritere());
        assertEquals(4, response.resultats().get(1).noteValue());
        assertEquals("Satisfait", response.resultats().get(1).noteLabel());
    }

    @Test
    void calculerEvaluation_missingProject_shouldThrowResourceNotFoundException() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> evaluationService.calculerEvaluation(1L, new EvaluationRequest(List.of(), List.of())));

        assertEquals("Projet introuvable avec id: 1", ex.getMessage());
    }

    @Test
    void calculerEvaluation_missingEliminatoryResult_shouldRejectRequest() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> evaluationService.calculerEvaluation(1L, new EvaluationRequest(null, null)));
    }

    @Test
    void calculerEvaluation_missingNoteResult_shouldRejectRequest() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));

        assertThrows(BadRequestException.class,
                () -> evaluationService.calculerEvaluation(1L, new EvaluationRequest(List.of(), List.of())));
    }

    @Test
    void calculerEvaluation_noteNull_shouldRejectRequest() {
        assertInvalidManualNote(null);
    }

    @Test
    void calculerEvaluation_noteNegative_shouldRejectRequest() {
        assertInvalidManualNote(-0.1);
    }

    @Test
    void calculerEvaluation_noteAboveBareme_shouldRejectRequest() {
        assertInvalidManualNote(6.0);
    }

    @Test
    void calculerEvaluation_withTenPointScale_shouldUseActiveMaxNoteValue() {
        when(noteLevelService.activeValues()).thenReturn(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculerEvaluation(1L, new EvaluationRequest(
                List.of(),
                List.of(new ResultatNoteRequest(2L, 7.0, "dynamic scale"))
        ));

        assertEquals(70.0, response.scoreFinal());
        assertEquals(7, response.resultats().get(0).noteValue());
        assertEquals(10.0, response.resultats().get(0).bareme());
        assertEquals(1.4, response.resultats().get(0).scorePondere());
    }

    @Test
    void calculerEvaluation_withoutActiveNoteLevels_shouldThrowBusinessConflict() {
        when(noteLevelService.activeValues()).thenThrow(new BusinessConflictException(NoteLevelService.NO_ACTIVE_NOTE_LEVEL_MESSAGE));
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> evaluationService.calculerEvaluation(1L, new EvaluationRequest(
                        List.of(),
                        List.of(new ResultatNoteRequest(2L, 3.0, "no scale"))
                )));

        assertEquals(NoteLevelService.NO_ACTIVE_NOTE_LEVEL_MESSAGE, ex.getMessage());
    }

    @Test
    void calculerEvaluation_invalidBareme_shouldBeIgnoredByScaleLogic() {
        critereNote.setBareme(0.0);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculerEvaluation(1L, new EvaluationRequest(
                List.of(),
                List.of(new ResultatNoteRequest(2L, 3.0, "scale"))
        ));

        assertEquals(60.0, response.scoreFinal());
        assertEquals(5.0, response.resultats().get(0).bareme());
    }

    @Test
    void calculerEvaluation_invalidPoids_shouldRejectRequest() {
        critereNote.setPoids(0.0);
        assertInvalidManualNote(1.0);
    }

    @Test
    void calculerEvaluation_eliminatoryNotOk_shouldWarnWithoutBlockingProject() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculerEvaluation(1L, new EvaluationRequest(
                List.of(new ResultatEliminatoireRequest(1L, ReponseEliminatoire.NOT_OK, "KO")),
                List.of(new ResultatNoteRequest(2L, 5.0, "Max"))
        ));

        assertEquals(100.0, response.scoreFinal());
        assertTrue(response.eligibleIndustrialisation());
        assertTrue(response.bloqueParEliminatoire());
        assertTrue(response.hasEliminatoryWarnings());
        assertEquals(1, response.eliminatoryWarningsCount());
        assertEquals(100.0, projet.getScoreFinalEvaluation());
        assertTrue(projet.getEligibleIndustrialisation());
        assertTrue(projet.getBloqueParEliminatoire());
        assertTrue(projet.getHasEliminatoryWarnings());
        assertEquals(1, projet.getEliminatoryWarningsCount());
        verify(sujetProjetRepository).save(projet);
    }

    @Test
    void calculateAutomaticEvaluation_adminCanRecalculateAnyTerminatedProject() {
        Livrable livrable = livrable("documentation-rapport-final.pdf", "Documentation rapport final", TypeLivrable.RAPPORT);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "Recalcul manuel");

        assertTrue(response.eligibleIndustrialisation());
        assertEquals("ROLE_ADMIN Admin Root", response.calculatedBy());
        assertEquals("Recalcul manuel", response.recalculationReason());
        assertEquals(100.0, response.scoreFinal());
    }

    @Test
    void calculateEvaluationForSystem_noCriteria_shouldPersistZeroScoreAsSystem() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of());
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateEvaluationForSystem(1L);

        assertEquals("SYSTEM", response.calculatedBy());
        assertEquals(0.0, response.scoreFinal());
        assertTrue(response.eligibleIndustrialisation());
        assertTrue(response.recalculationReason().contains("Calcul automatique"));
    }

    @Test
    void calculateAutomaticEvaluation_encadrantCanRecalculateOwnProject() {
        Livrable livrable = livrable("documentation-rapport-final.pdf", "Documentation rapport final", TypeLivrable.RAPPORT);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(livrable));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "Nouveaux livrables déposés");

        assertEquals("ROLE_ENSEIGNANT Sami Ben Ali", response.calculatedBy());
        assertEquals("Nouveaux livrables deposes", response.recalculationReason());
    }

    @Test
    void calculateAutomaticEvaluation_countMinNoRuleMatch_shouldUseDefaultNote() {
        critereNote.setModeEvaluation(ModeEvaluation.LIVRABLE_COUNT_MIN);
        critereNote.setMinLivrableCount(3);
        critereNote.setNoteMaxAuto(5.0);
        Livrable rapport = livrable("rapport.pdf", "Rapport", TypeLivrable.RAPPORT);
        Livrable doc = livrable("doc.pdf", "Documentation", TypeLivrable.DOCUMENTATION);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(rapport, doc));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "count");

        assertEquals(60.0, response.scoreFinal());
        assertEquals(3, response.resultats().get(0).noteValue());
        assertEquals("DEFAULT_NOTE", response.resultats().get(0).matchedRule());
        assertTrue(response.resultats().get(0).explanation().contains("note par defaut"));
    }

    @Test
    void calculateAutomaticEvaluation_noteMaxAutoBelowBareme_shouldBeIgnoredByRuleScale() {
        critereNote.setNoteMaxAuto(2.5);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L))
                .thenReturn(List.of(livrable("rapport.pdf", "Rapport", TypeLivrable.RAPPORT)));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "cap");

        assertEquals(100.0, response.scoreFinal());
        assertEquals(5, response.resultats().get(0).noteValue());
    }

    @Test
    void calculateAutomaticEvaluation_linkFileKeywordAndManualModes_shouldEvaluateEachMode() {
        CritereNote link = note(10L, ModeEvaluation.LIVRABLE_LINK_PRESENT, "Lien", null);
        CritereNote file = note(11L, ModeEvaluation.LIVRABLE_FILE_PRESENT, "Fichier", null);
        CritereNote keyword = note(12L, ModeEvaluation.KEYWORD_IN_LIVRABLE_NAME_OR_DESCRIPTION, "Keyword", null);
        keyword.setExpectedKeyword("docker");
        CritereNote manual = note(13L, ModeEvaluation.MANUAL_ADMIN_VALUE, "Manual", null);
        Livrable linked = livrable(null, "Repo", TypeLivrable.CODE_SOURCE);
        linked.setLienExterne("https://example.com/repo");
        Livrable filed = livrable("archive.pdf", "Archive", TypeLivrable.RAPPORT);
        filed.setObjectName("objects/archive.pdf");
        Livrable keywordLivrable = livrable("deploy.txt", "Pipeline Docker", TypeLivrable.FICHIER_TXT);

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(link, file, keyword, manual));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(linked, filed, keywordLivrable));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "modes");

        assertEquals(90.0, response.scoreFinal());
        assertEquals(5.0, response.resultats().get(0).noteObtenue());
        assertEquals(5.0, response.resultats().get(1).noteObtenue());
        assertEquals(5.0, response.resultats().get(2).noteObtenue());
        assertEquals(3.0, response.resultats().get(3).noteObtenue());
    }

    @Test
    void calculateAutomaticEvaluation_unconfiguredNote_shouldBeReportedAndIgnoredFromScore() {
        critereNote.setRuleEnabled(false);
        critereNote.setModeEvaluation(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of());
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "unconfigured note");

        assertEquals(60.0, response.scoreFinal());
        assertEquals(1, response.criteresNotesNonConfigures());
        assertFalse(response.resultats().get(0).ruleConfigured());
        assertEquals("DEFAULT_NOTE", response.resultats().get(0).matchedRule());
        assertTrue(response.scoreCalculationCoverageMessage().contains("1 critere(s)"));
    }

    @Test
    void calculateAutomaticEvaluation_encadrantCannotRecalculateAnotherProject() {
        projet.setEncadrantId(55L);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "Nouveaux livrables déposés"));
    }

    @Test
    void calculateAutomaticEvaluation_studentCannotRecalculateProject() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "Nouveaux livrables déposés"));
    }

    @Test
    void calculateAutomaticEvaluation_nonTerminatedProjectShouldFail() {
        projet.setStatut(StatutSujet.REALISATION_EN_COURS);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> evaluationService.calculateAutomaticEvaluation(1L, "Nouveaux livrables déposés"));

        assertEquals("Le projet doit etre en statut REALISATION_TERMINEE pour etre evalue.", ex.getMessage());
    }

    @Test
    void calculateAutomaticEvaluation_newLivrableImprovesScore() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L))
                .thenReturn(List.of());
        var before = evaluationService.calculateAutomaticEvaluation(1L, "Avant ajout");

        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L))
                .thenReturn(List.of(livrable("documentation-rapport-final.pdf", "Documentation rapport final", TypeLivrable.RAPPORT)));
        var after = evaluationService.calculateAutomaticEvaluation(1L, "Après ajout");

        assertTrue(after.scoreFinal() > before.scoreFinal());
        assertFalse(after.bloqueParEliminatoire());
    }

    @Test
    void calculateAutomaticEvaluation_unconfiguredCriterionShouldReturnWarningResult() {
        critereEliminatoire.setRuleEnabled(false);
        critereEliminatoire.setModeEvaluation(null);
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of());
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "Recalcul manuel");

        assertFalse(response.resultats().get(0).ruleConfigured());
        assertEquals("Aucune regle automatique configuree pour ce critere.", response.resultats().get(0).evidenceSummary());
        assertFalse(response.bloqueParEliminatoire());
        assertFalse(response.evaluationComplete());
        assertEquals(1, response.criteresEliminatoiresNonConfigures());
    }

    @Test
    void calculateAutomaticEvaluation_fullNotedScoreWithEliminatoryNotOk_shouldRemainEligibleWithWarnings() {
        critereEliminatoire.setModeEvaluation(ModeEvaluation.DOCUMENTATION_PRESENT);
        critereEliminatoire.setExpectedLivrableTypes("DOCUMENTATION,RAPPORT");
        critereNote.setModeEvaluation(ModeEvaluation.GIT_LINK_PRESENT);
        critereNote.setExpectedLivrableTypes("LIEN_GIT,CODE_SOURCE");
        critereNote.setNoteMaxAuto(5.0);
        Livrable git = livrable("repo.txt", "Git", TypeLivrable.LIEN_GIT);
        git.setLienExterne("https://github.com/esprit/demo");

        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(1L)).thenReturn(List.of(git));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = evaluationService.calculateAutomaticEvaluation(1L, "Recalcul manuel");

        assertEquals(100.0, response.scoreFinal());
        assertTrue(response.eligibleIndustrialisation());
        assertTrue(response.bloqueParEliminatoire());
        assertTrue(response.hasEliminatoryWarnings());
        assertEquals(1, response.eliminatoryWarningsCount());
        assertEquals(List.of("Documentation"), response.blockingCriteriaNames());
    }

    @Test
    void getLatestEvaluationByProjet_noEvaluation_shouldThrowResourceNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> evaluationService.getLatestEvaluationByProjet(1L));

        assertEquals("Aucune evaluation trouvee pour le projet id: 1", ex.getMessage());
    }

    @Test
    void getLatestEvaluationByProjet_ciCanReadAndInvalidEvidenceIdsAreIgnored() {
        User ci = new User();
        ci.setId(77L);
        ci.setPrenom("CI");
        ci.setNom("User");
        ci.setRole(Role.ROLE_CI);
        Evaluation evaluation = savedEvaluationWithResult("7,abc,8");
        when(userService.getCurrentUser()).thenReturn(ci);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(1L)).thenReturn(Optional.of(evaluation));

        var response = evaluationService.getLatestEvaluationByProjet(1L);

        assertEquals(List.of(7L, 8L), response.resultats().get(0).evidenceLivrableIds());
        assertTrue(response.evaluationComplete());
    }

    @Test
    void getLatestEvaluationByProjet_ownerTeacherCanRead() {
        Evaluation evaluation = savedEvaluationWithResult("101");
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(1L)).thenReturn(Optional.of(evaluation));

        var response = evaluationService.getLatestEvaluationByProjet(1L);

        assertEquals(1L, response.sujetProjetId());
        assertEquals(List.of(101L), response.resultats().get(0).evidenceLivrableIds());
    }

    @Test
    void getLatestEvaluationByProjet_studentShouldBeDenied() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class, () -> evaluationService.getLatestEvaluationByProjet(1L));
    }

    private void assertInvalidManualNote(Double note) {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));

        assertThrows(BadRequestException.class,
                () -> evaluationService.calculerEvaluation(1L, new EvaluationRequest(
                        List.of(),
                        List.of(new ResultatNoteRequest(2L, note, "invalid"))
                )));
    }

    private CritereNote note(Long id, ModeEvaluation mode, String label, String expectedTypes) {
        CritereNote note = new CritereNote();
        note.setId(id);
        note.setLibelle(label);
        note.setDomaine(label);
        note.setActif(true);
        note.setOrdre(id.intValue());
        note.setBareme(5.0);
        note.setPoids(1.0);
        note.setRuleEnabled(true);
        note.setModeEvaluation(mode);
        note.setExpectedLivrableTypes(expectedTypes);
        note.setNoteMaxAuto(5.0);
        note.setDefaultNoteValue(3);
        notesById.put(id, note);
        return note;
    }

    private List<CritereNoteRule> legacyRulesFor(CritereNote critere) {
        if (critere == null || critere.getModeEvaluation() == null || !Boolean.TRUE.equals(critere.getRuleEnabled())) {
            return List.of();
        }
        CritereNoteRule rule = new CritereNoteRule();
        rule.setId(900L + critere.getId());
        rule.setCritereNote(critere);
        rule.setRuleName("Legacy metadata rule");
        rule.setDescription("Regle convertie pour le test");
        rule.setPriority(1);
        rule.setActive(true);
        rule.setNoteValue(5);
        switch (critere.getModeEvaluation()) {
            case DOCUMENTATION_PRESENT -> {
                rule.setMetadataKey("hasDocumentation");
                rule.setOperator(RuleOperator.EQUALS);
                rule.setExpectedValue("true");
            }
            case GIT_LINK_PRESENT -> {
                rule.setMetadataKey("hasGitLink");
                rule.setOperator(RuleOperator.EQUALS);
                rule.setExpectedValue("true");
            }
            case LIVRABLE_TYPE_PRESENT -> {
                rule.setMetadataKey("livrableTypes");
                rule.setOperator(RuleOperator.CONTAINS);
                rule.setExpectedValue(critere.getExpectedLivrableTypes());
            }
            case LIVRABLE_LINK_PRESENT -> {
                rule.setMetadataKey("externalLinkCount");
                rule.setOperator(RuleOperator.GTE);
                rule.setMinValue(1.0);
            }
            case LIVRABLE_FILE_PRESENT -> {
                rule.setMetadataKey("fileCount");
                rule.setOperator(RuleOperator.GTE);
                rule.setMinValue(1.0);
            }
            case LIVRABLE_COUNT_MIN -> {
                rule.setMetadataKey("livrableCount");
                rule.setOperator(RuleOperator.GTE);
                rule.setMinValue((double) (critere.getMinLivrableCount() == null ? 1 : critere.getMinLivrableCount()));
            }
            case KEYWORD_IN_LIVRABLE_NAME_OR_DESCRIPTION -> {
                rule.setMetadataKey("livrableSearchText");
                rule.setOperator(RuleOperator.CONTAINS);
                rule.setExpectedValue(critere.getExpectedKeyword());
            }
            case MANUAL_ADMIN_VALUE -> {
                return List.of();
            }
        }
        return List.of(rule);
    }

    private Evaluation savedEvaluationWithResult(String evidenceIds) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(20L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(80.0);
        evaluation.setEligibleIndustrialisation(true);
        evaluation.setBloqueParEliminatoire(false);
        ResultatCritere resultat = new ResultatCritere();
        resultat.setId(30L);
        resultat.setCritereId(2L);
        resultat.setCritereLibelle("Rapport");
        resultat.setTypeCritere(TypeCritere.NOTE);
        resultat.setRuleConfigured(true);
        resultat.setModeEvaluation(ModeEvaluation.LIVRABLE_TYPE_PRESENT);
        resultat.setNoteObtenue(4.0);
        resultat.setNoteValue(4);
        resultat.setNoteLabel("Satisfait");
        resultat.setScorePondere(1.6);
        resultat.setBareme(5.0);
        resultat.setPoids(2.0);
        resultat.setEvidenceLivrableIds(evidenceIds);
        evaluation.addResultat(resultat);
        return evaluation;
    }

    private Livrable livrable(String fileName, String nom, TypeLivrable typeLivrable) {
        Livrable livrable = new Livrable();
        livrable.setId(100L + typeLivrable.ordinal());
        livrable.setOriginalFileName(fileName);
        livrable.setNom(nom);
        livrable.setDescription(nom);
        livrable.setTypeLivrable(typeLivrable);
        livrable.setActif(true);
        return livrable;
    }
}
