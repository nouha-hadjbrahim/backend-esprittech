package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionRecommandeeIndustrialisation;
import tn.esprit.backend_esprittech.entity.critereeval.Evaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.critereeval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.critereeval.NoteLevelService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustrialisationScoreServiceTest {

    @Mock private CandidatureIndustrialisationRepository candidatureRepository;
    @Mock private QuestionIndustrialisationRepository questionRepository;
    @Mock private ReponseIndustrialisationRepository reponseRepository;
    @Mock private EvaluationRepository evaluationRepository;
    @Mock private ProjetCatalogueRepository projetCatalogueRepository;
    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private NoteLevelService noteLevelService;

    @InjectMocks
    private IndustrialisationScoreService service;

    private SujetProjet sujet;
    private CandidatureIndustrialisation candidature;

    @BeforeEach
    void setUp() {
        sujet = new SujetProjet();
        sujet.setId(42L);
        sujet.setTitre("Plateforme IoT");
        sujet.setScoreFinalEvaluation(90.0);
        sujet.setEligibleIndustrialisation(true);
        sujet.setBloqueParEliminatoire(false);

        candidature = new CandidatureIndustrialisation();
        candidature.setId(11L);
        candidature.setSujetProjet(sujet);
    }

    @Test
    void calculate_shouldUseLatestMlEvaluationAsFinalScore() {
        QuestionIndustrialisation question = question(1L);
        ReponseIndustrialisation reponse = response(question, true);
        mockSujetBase(List.of(question), List.of(reponse), Optional.of(evaluationWithNote(82.0, sujet)));

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
        Evaluation evaluation = evaluationWithNote(64.0, sujet);
        evaluation.setFinalValidatedScore(91.5);
        mockSujetBase(List.of(), List.of(), Optional.of(evaluation));

        var score = service.calculate(11L);

        assertEquals(91.5, score.scoreFinal());
        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "SCORE_VALIDE".equals(detail.statut())));
    }

    @Test
    void calculate_shouldExposeEvaluationEliminatoryWarningsWithoutChangingNumericScore() {
        Evaluation evaluation = evaluationWithNote(88.0, sujet);
        ResultatCritere eliminatory = new ResultatCritere();
        eliminatory.setCritereId(77L);
        eliminatory.setCritereLibelle("Licence incompatible");
        eliminatory.setTypeCritere(TypeCritere.ELIMINATOIRE);
        eliminatory.setRuleConfigured(true);
        eliminatory.setReponseEliminatoire(ReponseEliminatoire.NOT_OK);
        evaluation.addResultat(eliminatory);
        mockSujetBase(List.of(), List.of(), Optional.of(evaluation));

        var score = service.calculate(11L);

        assertEquals(88.0, score.scoreFinal());
        assertFalse(score.estBloqueParEliminatoire());
        assertTrue(score.hasEliminatoryWarnings());
        assertEquals("Licence incompatible", score.blocagesEliminatoires().get(0).libelle());
        assertTrue(score.message().contains("Alerte eliminatoire"));
    }

    @Test
    void calculate_withoutDetailedEvaluation_shouldUseProjectSnapshotWhenPresent() {
        mockSujetBase(List.of(), List.of(), Optional.empty());

        var score = service.calculate(11L);

        assertEquals(90.0, score.scoreFinal());
        assertEquals(90.0, score.scoreCriteresNotes());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "SNAPSHOT_PROJET".equals(detail.statut())));
    }

    @Test
    void calculate_withoutEvaluationOrSnapshot_shouldReturnNoGoAndMissingEvaluation() {
        sujet.setScoreFinalEvaluation(null);
        mockSujetBase(List.of(), List.of(), Optional.empty());

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

    @Test
    void calculate_sujetOnlyBeforeConversion_shouldResolveSujetEvaluation() {
        mockSujetBase(List.of(), List.of(), Optional.of(evaluationWithNote(76.0, sujet)));

        var score = service.calculate(11L);

        assertEquals(76.0, score.scoreFinal());
        assertEquals(42L, score.projetId());
        verify(evaluationRepository).findTopBySujetProjetIdOrderByDateCalculDescIdDesc(42L);
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void calculate_convertedCandidatureWithNullSujet_shouldUseProjetCatalogueEvaluation() {
        ProjetCatalogue catalogue = catalogue(55L, null);
        CandidatureIndustrialisation converted = new CandidatureIndustrialisation();
        converted.setId(18L);
        converted.setSujetProjet(null);
        converted.setProjetCatalogue(catalogue);

        Evaluation evaluation = evaluationWithNote(84.0, null);
        evaluation.setProjetCatalogue(catalogue);
        evaluation.setId(99L);

        when(candidatureRepository.findById(18L)).thenReturn(Optional.of(converted));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(55L))
                .thenReturn(Optional.of(evaluation));

        var score = service.calculate(18L);

        assertEquals(84.0, score.scoreFinal());
        assertEquals(55L, score.projetId());
        assertEquals(18L, score.candidatureId());
        assertTrue(score.detailsCalcul().stream().anyMatch(d -> "Qualite technique".equals(d.libelle())));
        verify(evaluationRepository, never()).save(any());
        verify(evaluationRepository, never()).delete(any());
    }

    @Test
    void calculate_legacyCandidature_shouldResolveProjetViaSujetRelation() {
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(42L))
                .thenReturn(Optional.empty());

        ProjetCatalogue catalogue = catalogue(70L, 42L);
        Evaluation catalogueEval = evaluationWithNote(79.0, null);
        catalogueEval.setProjetCatalogue(catalogue);
        when(projetCatalogueRepository.findBySujetId(42L)).thenReturn(Optional.of(catalogue));
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(70L))
                .thenReturn(Optional.of(catalogueEval));

        var score = service.calculate(11L);

        assertEquals(79.0, score.scoreFinal());
        assertEquals(42L, score.projetId());
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void calculate_catalogueWithoutOwnEval_shouldFallbackToOriginSujetEvaluation() {
        ProjetCatalogue catalogue = catalogue(55L, 42L);
        CandidatureIndustrialisation converted = new CandidatureIndustrialisation();
        converted.setId(18L);
        converted.setProjetCatalogue(catalogue);

        Evaluation sujetEval = evaluationWithNote(81.0, sujet);
        when(candidatureRepository.findById(18L)).thenReturn(Optional.of(converted));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(55L))
                .thenReturn(Optional.empty());
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(42L))
                .thenReturn(Optional.of(sujetEval));

        var score = service.calculate(18L);

        assertEquals(81.0, score.scoreFinal());
        assertEquals(55L, score.projetId());
    }

    @Test
    void calculate_candidatureWithoutRelation_shouldThrowBusinessException() {
        CandidatureIndustrialisation orphan = new CandidatureIndustrialisation();
        orphan.setId(19L);
        when(candidatureRepository.findById(19L)).thenReturn(Optional.of(orphan));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.calculate(19L));
        assertTrue(ex.getMessage().contains("ni a un sujet ni a un projet catalogue"));
    }

    @Test
    void calculate_existingEvaluation_shouldKeepScoreAndCriterionDetailsUnchanged() {
        Evaluation evaluation = evaluationWithNote(88.0, sujet);
        mockSujetBase(List.of(), List.of(), Optional.of(evaluation));

        var first = service.calculate(11L);
        var second = service.calculate(11L);

        assertEquals(first.scoreFinal(), second.scoreFinal());
        assertEquals(first.detailsCalcul().size(), second.detailsCalcul().size());
        assertEquals(
                first.detailsCalcul().stream().filter(d -> "CRITERES_NOTES".equals(d.composant())).count(),
                second.detailsCalcul().stream().filter(d -> "CRITERES_NOTES".equals(d.composant())).count()
        );
        verify(evaluationRepository, never()).save(any());
        verify(evaluationRepository, never()).delete(any());
        verify(evaluationRepository, never()).deleteById(anyLong());
    }

    @Test
    void resolveEvaluationProject_convertedCandidature_prefersProjetCatalogue() {
        ProjetCatalogue catalogue = catalogue(55L, 42L);
        candidature.setSujetProjet(null);
        candidature.setProjetCatalogue(catalogue);
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(55L))
                .thenReturn(Optional.of(evaluationWithNote(70.0, null)));

        var context = service.resolveEvaluationProject(candidature);

        assertEquals(55L, context.displayProjetId());
        assertTrue(context.latestEvaluation().isPresent());
        assertEquals(70.0, context.latestEvaluation().get().getScoreFinal());
    }

    @Test
    void calculate_catalogueWithScoreSnapshotOnly_shouldUseCatalogueScore() {
        ProjetCatalogue catalogue = catalogue(55L, null);
        catalogue.setScore(77);
        CandidatureIndustrialisation converted = new CandidatureIndustrialisation();
        converted.setId(18L);
        converted.setProjetCatalogue(catalogue);

        when(candidatureRepository.findById(18L)).thenReturn(Optional.of(converted));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(55L))
                .thenReturn(Optional.empty());

        var score = service.calculate(18L);

        assertEquals(77.0, score.scoreFinal());
        assertEquals(55L, score.projetId());
        assertTrue(score.detailsCalcul().stream().anyMatch(d -> "SNAPSHOT_PROJET".equals(d.statut())));
        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void calculate_catalogueWithZeroScore_shouldFallbackToSujetSnapshot() {
        ProjetCatalogue catalogue = catalogue(55L, 42L);
        catalogue.setScore(0);
        sujet.setScoreFinalEvaluation(66.0);
        CandidatureIndustrialisation converted = new CandidatureIndustrialisation();
        converted.setId(18L);
        converted.setProjetCatalogue(catalogue);

        when(candidatureRepository.findById(18L)).thenReturn(Optional.of(converted));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
        when(reponseRepository.findByCandidatureIndustrialisationId(18L)).thenReturn(List.of());
        when(evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(55L))
                .thenReturn(Optional.empty());
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(42L))
                .thenReturn(Optional.empty());
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(sujet));

        var score = service.calculate(18L);

        assertEquals(66.0, score.scoreFinal());
        assertEquals(55L, score.projetId());
    }

    private void mockSujetBase(List<QuestionIndustrialisation> questions,
                               List<ReponseIndustrialisation> responses,
                               Optional<Evaluation> evaluation) {
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(questions);
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(responses);
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(42L)).thenReturn(evaluation);
    }

    private ProjetCatalogue catalogue(Long id, Long sujetId) {
        ProjetCatalogue catalogue = new ProjetCatalogue();
        catalogue.setId(id);
        catalogue.setTitre("Catalogue projet");
        catalogue.setSujetId(sujetId);
        catalogue.setScore(0);
        return catalogue;
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

    private Evaluation evaluationWithNote(double normalizedScore, SujetProjet linkedSujet) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(linkedSujet);
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
