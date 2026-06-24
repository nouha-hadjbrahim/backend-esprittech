package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatNoteRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    @InjectMocks
    private EvaluationService evaluationService;

    private SujetProjet projet;
    private CritereEliminatoire critereEliminatoire;
    private CritereNote critereNote;

    @BeforeEach
    void setUp() {
        projet = new SujetProjet();
        projet.setId(1L);
        projet.setStatut(StatutSujet.REALISATION_TERMINEE);

        critereEliminatoire = new CritereEliminatoire();
        critereEliminatoire.setId(1L);
        critereEliminatoire.setActif(true);
        critereEliminatoire.setOrdre(1);

        critereNote = new CritereNote();
        critereNote.setId(2L);
        critereNote.setActif(true);
        critereNote.setOrdre(1);
        critereNote.setBareme(5.0);
        critereNote.setPoids(2.0);
    }

    @Test
    void calculerEvaluation_allCriteriaOk_shouldReturnEligibleAndScoreComputed() {
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
        assertEquals("Projet évalué avec succès.", response.commentaire());
        assertEquals(2, response.resultats().size());
        assertEquals(TypeCritere.ELIMINATOIRE, response.resultats().get(0).typeCritere());
        assertEquals(TypeCritere.NOTE, response.resultats().get(1).typeCritere());
    }

    @Test
    void calculerEvaluation_notOkEliminatoire_shouldReturnNotEligible() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationRequest request = new EvaluationRequest(
                List.of(new ResultatEliminatoireRequest(1L, ReponseEliminatoire.NOT_OK, "Non éligible")),
                List.of(new ResultatNoteRequest(2L, 3.0, "Note commentaire"))
        );

        var response = evaluationService.calculerEvaluation(1L, request);

        assertFalse(response.eligibleIndustrialisation());
        assertTrue(response.bloqueParEliminatoire());
        assertEquals("Projet non éligible : au moins un critère éliminatoire est NOT_OK.", response.commentaire());
    }

    @Test
    void calculerEvaluation_noteAboveBareme_shouldThrowBadRequestException() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));

        EvaluationRequest request = new EvaluationRequest(
                List.of(new ResultatEliminatoireRequest(1L, ReponseEliminatoire.OK, null)),
                List.of(new ResultatNoteRequest(2L, 6.0, "Note trop haute"))
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> evaluationService.calculerEvaluation(1L, request));

        assertEquals("La note obtenue ne peut pas dépasser le barème du critère id: 2", ex.getMessage());
    }

    @Test
    void calculerEvaluation_projectNotTerminated_shouldThrowBadRequestException() {
        projet.setStatut(StatutSujet.REALISATION_EN_COURS);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));

        EvaluationRequest request = new EvaluationRequest(List.of(), List.of());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> evaluationService.calculerEvaluation(1L, request));

        assertEquals("Le projet doit être en statut REALISATION_TERMINEE pour être évalué.", ex.getMessage());
    }

    @Test
    void calculerEvaluation_missingActiveCriterionResult_shouldThrowBadRequestException() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(projet));
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereEliminatoire));
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(critereNote));

        EvaluationRequest request = new EvaluationRequest(
                List.of(new ResultatEliminatoireRequest(1L, ReponseEliminatoire.OK, null)),
                List.of()
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> evaluationService.calculerEvaluation(1L, request));

        assertEquals("Résultat manquant pour le critère noté id: 2", ex.getMessage());
    }

    @Test
    void getLatestEvaluationByProjet_noEvaluation_shouldThrowResourceNotFoundException() {
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> evaluationService.getLatestEvaluationByProjet(1L));

        assertEquals("Aucune évaluation trouvée pour le projet id: 1", ex.getMessage());
    }
}
