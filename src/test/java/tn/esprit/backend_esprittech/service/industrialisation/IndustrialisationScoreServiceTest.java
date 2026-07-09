package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionRecommandeeIndustrialisation;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.service.CritereEval.NoteLevelService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustrialisationScoreServiceTest {

    @Mock private CandidatureIndustrialisationRepository candidatureRepository;
    @Mock private QuestionIndustrialisationRepository questionRepository;
    @Mock private ReponseIndustrialisationRepository reponseRepository;
    @Mock private EvaluationRepository evaluationRepository;
    @Mock private NoteLevelService noteLevelService;

    @InjectMocks
    private IndustrialisationScoreService service;

    private SujetProjet projet;
    private CandidatureIndustrialisation candidature;

    @BeforeEach
    void setUp() {
        projet = new SujetProjet();
        projet.setId(42L);
        projet.setTitre("Plateforme IoT");
        projet.setScoreFinalEvaluation(90.0);
        projet.setEligibleIndustrialisation(true);
        projet.setBloqueParEliminatoire(false);

        candidature = new CandidatureIndustrialisation();
        candidature.setId(11L);
        candidature.setSujetProjet(projet);
    }

    @Test
    void calculate_shouldUseLatestMlEvaluationAsFinalScore() {
        QuestionIndustrialisation question = question(1L);
        ReponseIndustrialisation reponse = response(question, true);
        mockBase(List.of(question), List.of(reponse), Optional.of(evaluationWithNote(82.0)));

        var score = service.calculate(11L);

        assertEquals(82.0, score.scoreFinal());
        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
        assertEquals(100.0, score.scoreQuestions());
        assertEquals(0.0, score.scoreLivrables());
        assertEquals(82.0, score.scoreCriteresNotes());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail ->
                "COUVERT_PAR_EVALUATION_ML".equals(detail.statut())));
    }

    @Test
    void calculate_shouldPreferValidatedFinalScoreOverMlScore() {
        Evaluation evaluation = evaluationWithNote(64.0);
        evaluation.setFinalValidatedScore(91.5);
        mockBase(List.of(), List.of(), Optional.of(evaluation));

        var score = service.calculate(11L);

        assertEquals(91.5, score.scoreFinal());
        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "SCORE_VALIDE".equals(detail.statut())));
    }

    @Test
    void calculate_shouldExposeEvaluationEliminatoryWarningsWithoutChangingNumericScore() {
        Evaluation evaluation = evaluationWithNote(88.0);
        ResultatCritere eliminatory = new ResultatCritere();
        eliminatory.setCritereId(77L);
        eliminatory.setCritereLibelle("Licence incompatible");
        eliminatory.setTypeCritere(TypeCritere.ELIMINATOIRE);
        eliminatory.setRuleConfigured(true);
        eliminatory.setReponseEliminatoire(ReponseEliminatoire.NOT_OK);
        evaluation.addResultat(eliminatory);
        mockBase(List.of(), List.of(), Optional.of(evaluation));

        var score = service.calculate(11L);

        assertEquals(88.0, score.scoreFinal());
        assertFalse(score.estBloqueParEliminatoire());
        assertTrue(score.hasEliminatoryWarnings());
        assertEquals("Licence incompatible", score.blocagesEliminatoires().get(0).libelle());
        assertTrue(score.message().contains("Alerte eliminatoire"));
    }

    @Test
    void calculate_withoutDetailedEvaluation_shouldUseProjectSnapshotWhenPresent() {
        mockBase(List.of(), List.of(), Optional.empty());

        var score = service.calculate(11L);

        assertEquals(90.0, score.scoreFinal());
        assertEquals(90.0, score.scoreCriteresNotes());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "SNAPSHOT_PROJET".equals(detail.statut())));
    }

    @Test
    void calculate_withoutEvaluationOrSnapshot_shouldReturnNoGoAndMissingEvaluation() {
        projet.setScoreFinalEvaluation(null);
        mockBase(List.of(), List.of(), Optional.empty());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreFinal());
        assertEquals(DecisionRecommandeeIndustrialisation.NO_GO, score.decisionRecommandee());
        assertTrue(score.elementsManquants().stream().anyMatch(item -> item.contains("Aucun critere actif")));
    }

    @Test
    void calculate_unknownCandidature_shouldThrowResourceNotFound() {
        when(candidatureRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.calculate(404L));

        assertEquals("Demande industrialisation introuvable avec id: 404", ex.getMessage());
    }

    private void mockBase(List<QuestionIndustrialisation> questions,
                          List<ReponseIndustrialisation> responses,
                          Optional<Evaluation> evaluation) {
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(questions);
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(responses);
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(42L)).thenReturn(evaluation);
    }

    private QuestionIndustrialisation question(Long id) {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setId(id);
        question.setLibelle("Question " + id);
        question.setTypeCritere(TypeCritere.NOTE);
        question.setTypeReponse(TypeReponseIndustrialisation.BOOLEAN);
        question.setPoids(1.0);
        question.setObligatoire(true);
        question.setActif(true);
        question.setOrdre(id.intValue());
        return question;
    }

    private ReponseIndustrialisation response(QuestionIndustrialisation question, boolean value) {
        ReponseIndustrialisation response = new ReponseIndustrialisation();
        response.setQuestion(question);
        response.setCandidatureIndustrialisation(candidature);
        response.setValeurBoolean(value);
        return response;
    }

    private Evaluation evaluationWithNote(double normalizedScore) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(normalizedScore);
        evaluation.setMlScore(normalizedScore);
        evaluation.setMlStatus("COMPLETED");
        evaluation.setEligibleIndustrialisation(true);
        evaluation.setBloqueParEliminatoire(false);

        ResultatCritere result = new ResultatCritere();
        result.setCritereId(20L);
        result.setCritereLibelle("Qualite technique");
        result.setTypeCritere(TypeCritere.NOTE);
        result.setModeEvaluation(ModeEvaluation.CONTENT_ANALYSIS);
        result.setRuleConfigured(true);
        result.setNoteValue((int) Math.round(normalizedScore / 20.0));
        result.setNoteObtenue(normalizedScore / 20.0);
        result.setNormalizedScore(normalizedScore / 100.0);
        result.setBareme(5.0);
        result.setPoids(1.0);
        result.setEvidenceSummary("Evidence ML contenu");
        evaluation.addResultat(result);
        return evaluation;
    }
}
