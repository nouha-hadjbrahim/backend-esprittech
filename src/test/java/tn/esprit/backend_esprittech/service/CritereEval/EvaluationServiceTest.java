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
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private UserService userService;

    @InjectMocks
    private EvaluationService evaluationService;

    private SujetProjet projet;
    private CritereEliminatoire critereEliminatoire;
    private CritereNote critereNote;
    private User admin;
    private User encadrant;
    private User etudiant;

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
        assertEquals("Nouveaux livrables déposés", response.recalculationReason());
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

        assertEquals("Le projet doit être en statut REALISATION_TERMINEE pour être évalué.", ex.getMessage());
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
    void calculateAutomaticEvaluation_fullNotedScoreWithEliminatoryNotOk_shouldRemainNoGo() {
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
        assertFalse(response.eligibleIndustrialisation());
        assertTrue(response.bloqueParEliminatoire());
        assertEquals(List.of("Documentation"), response.blockingCriteriaNames());
    }

    @Test
    void getLatestEvaluationByProjet_noEvaluation_shouldThrowResourceNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> evaluationService.getLatestEvaluationByProjet(1L));

        assertEquals("Aucune évaluation trouvée pour le projet id: 1", ex.getMessage());
    }

    private Livrable livrable(String fileName, String nom, TypeLivrable typeLivrable) {
        Livrable livrable = new Livrable();
        livrable.setOriginalFileName(fileName);
        livrable.setNom(nom);
        livrable.setDescription(nom);
        livrable.setTypeLivrable(typeLivrable);
        livrable.setActif(true);
        return livrable;
    }
}
