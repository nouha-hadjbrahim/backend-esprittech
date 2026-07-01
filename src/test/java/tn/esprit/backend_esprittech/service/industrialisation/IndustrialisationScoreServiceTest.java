package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionRecommandeeIndustrialisation;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.service.CritereEval.NoteLevelService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustrialisationScoreServiceTest {

    @Mock
    private CandidatureIndustrialisationRepository candidatureRepository;

    @Mock
    private QuestionIndustrialisationRepository questionRepository;

    @Mock
    private ReponseIndustrialisationRepository reponseRepository;

    @Mock
    private LivrableRepository livrableRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private CritereNoteRepository critereNoteRepository;

    @Mock
    private CritereEliminatoireRepository critereEliminatoireRepository;

    @Mock
    private NoteLevelService noteLevelService;

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
    void calculate_allCriteriaOkAndScoreAbove80_shouldRecommendGo() {
        QuestionIndustrialisation question = question(1L, "API documentee", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 1.0, true);
        ReponseIndustrialisation reponse = response(question);
        reponse.setValeurBoolean(true);
        Livrable git = livrable(TypeLivrable.LIEN_GIT, "https://github.com/esprit/demo", null);

        mockBase(List.of(question), List.of(reponse), List.of(git), Optional.of(evaluationWithNote(90.0)), List.of(livrableRule(TypeLivrable.LIEN_GIT)));

        var score = service.calculate(11L);

        assertEquals(97.0, score.scoreFinal());
        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
        assertFalse(score.estBloqueParEliminatoire());
    }

    @Test
    void calculate_unknownCandidature_shouldThrowResourceNotFound() {
        when(candidatureRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.calculate(404L));

        assertEquals("Demande industrialisation introuvable avec id: 404", ex.getMessage());
    }

    @Test
    void calculate_scoreBetween60And80WithoutBlock_shouldRecommendInstruction() {
        QuestionIndustrialisation question = question(1L, "Question partielle", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 1.0, true);
        ReponseIndustrialisation reponse = response(question);
        reponse.setValeurBoolean(true);

        mockBase(List.of(question), List.of(reponse), List.of(livrable(TypeLivrable.RAPPORT, null, "rapport.pdf")),
                Optional.of(evaluationWithNote(60.0)), List.of());

        var score = service.calculate(11L);

        assertEquals(79.0, score.scoreFinal());
        assertEquals(DecisionRecommandeeIndustrialisation.A_INSTRUIRE, score.decisionRecommandee());
        assertEquals("A instruire : score intermediaire, verification CI requise.", score.message());
    }

    @Test
    void calculate_eliminatoryNotOkWithHighNumericScore_shouldExposeWarningAndKeepNumericRecommendation() {
        QuestionIndustrialisation note = question(1L, "Documentation", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 1.0, true);
        ReponseIndustrialisation noteResponse = response(note);
        noteResponse.setValeurBoolean(true);
        QuestionIndustrialisation eliminatory = question(2L, "Depot Git disponible ?", TypeCritere.ELIMINATOIRE, TypeReponseIndustrialisation.BOOLEAN, null, true);
        ReponseIndustrialisation badResponse = response(eliminatory);
        badResponse.setValeurBoolean(false);
        Livrable git = livrable(TypeLivrable.LIEN_GIT, "https://github.com/esprit/demo", null);

        mockBase(List.of(note, eliminatory), List.of(noteResponse, badResponse), List.of(git), Optional.of(evaluationWithNote(100.0)), List.of(livrableRule(TypeLivrable.LIEN_GIT)));

        var score = service.calculate(11L);

        assertEquals(100.0, score.scoreFinal());
        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
        assertFalse(score.estBloqueParEliminatoire());
        assertTrue(score.hasEliminatoryWarnings());
        assertEquals(1, score.eliminatoryWarningsCount());
        assertEquals("Depot Git disponible ?", score.blocagesEliminatoires().get(0).libelle());
    }

    @Test
    void calculate_optionalEliminatoryWithoutAnswer_shouldStayPendingWithoutBlock() {
        QuestionIndustrialisation optional = question(2L, "Depot optionnel", TypeCritere.ELIMINATOIRE, TypeReponseIndustrialisation.BOOLEAN, null, false);
        mockBase(List.of(optional), List.of(), List.of(), Optional.of(evaluationWithNote(80.0)), List.of());

        var score = service.calculate(11L);

        assertFalse(score.estBloqueParEliminatoire());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "EN_ATTENTE".equals(detail.statut())));
    }

    @Test
    void calculate_projectEvaluationEliminatoryNotOk_shouldExposeWarningWithoutForcingNoGo() {
        Evaluation evaluation = evaluationWithNote(95.0);
        ResultatCritere eliminatory = new ResultatCritere();
        eliminatory.setCritereId(77L);
        eliminatory.setCritereLibelle("Licence incompatible");
        eliminatory.setTypeCritere(TypeCritere.ELIMINATOIRE);
        eliminatory.setRuleConfigured(true);
        eliminatory.setReponseEliminatoire(ReponseEliminatoire.NOT_OK);
        evaluation.addResultat(eliminatory);
        mockBase(List.of(), List.of(), List.of(livrable(TypeLivrable.RAPPORT, null, "rapport.pdf")), Optional.of(evaluation), List.of());

        var score = service.calculate(11L);

        assertFalse(score.estBloqueParEliminatoire());
        assertTrue(score.hasEliminatoryWarnings());
        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
        assertEquals("Licence incompatible", score.blocagesEliminatoires().get(0).libelle());
        assertTrue(score.message().contains("Alerte eliminatoire"));
    }

    @Test
    void calculate_missingMandatoryLivrable_shouldReduceLivrableScore() {
        mockBase(List.of(), List.of(), List.of(), Optional.empty(), List.of(livrableRule(TypeLivrable.RAPPORT)));

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreLivrables());
        assertTrue(score.elementsManquants().stream().anyMatch(item -> item.contains("RAPPORT")));
        assertEquals(DecisionRecommandeeIndustrialisation.NO_GO, score.decisionRecommandee());
    }

    @Test
    void calculate_livrableRuleFromEliminatoryCriteria_shouldRequireConfiguredType() {
        CritereEliminatoire eliminatoryRule = livrableEliminatoryRule(TypeLivrable.PRESENTATION);
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluationWithNote(80.0)), List.of(), List.of(eliminatoryRule));

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreLivrables());
        assertTrue(score.elementsManquants().contains("Livrable obligatoire manquant: PRESENTATION"));
    }

    @Test
    void calculate_documentationAndGitFallbackRules_shouldMatchReportAndGitLikeCode() {
        CritereNote documentation = ruleWithMode(ModeEvaluation.DOCUMENTATION_PRESENT, null);
        CritereNote git = ruleWithMode(ModeEvaluation.GIT_LINK_PRESENT, null);
        Livrable report = livrable(TypeLivrable.RAPPORT, null, "rapport.pdf");
        Livrable code = livrable(TypeLivrable.CODE_SOURCE, "https://gitlab.com/esprit/demo", null);

        mockBase(List.of(), List.of(), List.of(report, code), Optional.of(evaluationWithNote(80.0)), List.of(documentation, git));

        var score = service.calculate(11L);

        assertEquals(100.0, score.scoreLivrables());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "DOCUMENTATION".equals(detail.reference())));
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "LIEN_GIT".equals(detail.reference())));
    }

    @Test
    void calculate_invalidExpectedTypes_shouldFallbackToGlobalLivrableScore() {
        CritereNote invalidRule = ruleWithMode(ModeEvaluation.LIVRABLE_TYPE_PRESENT, "UNKNOWN");

        mockBase(List.of(), List.of(), List.of(livrable(TypeLivrable.RAPPORT, null, "rapport.pdf")),
                Optional.of(evaluationWithNote(80.0)), List.of(invalidRule));

        var score = service.calculate(11L);

        assertEquals(70.0, score.scoreLivrables());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "CONFIGURATION_INCOMPLETE".equals(detail.statut())));
    }

    @Test
    void calculate_invalidLivrableAccess_shouldKeepPresenceScoreWithoutAccessBonus() {
        mockBase(List.of(), List.of(), List.of(livrable(TypeLivrable.RAPPORT, null, "rapport.exe")),
                Optional.of(evaluationWithNote(80.0)), List.of(livrableRule(TypeLivrable.RAPPORT)));

        var score = service.calculate(11L);

        assertEquals(90.0, score.scoreLivrables());
    }

    @Test
    void calculate_livrableWithoutFileOrLink_shouldUseNameAsEvidence() {
        Livrable livrable = livrable(TypeLivrable.RAPPORT, null, null);
        livrable.setNom("Rapport oral");
        mockBase(List.of(), List.of(), List.of(livrable), Optional.of(evaluationWithNote(80.0)), List.of(livrableRule(TypeLivrable.RAPPORT)));

        var score = service.calculate(11L);

        assertEquals(90.0, score.scoreLivrables());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> detail.details().contains("Rapport oral")));
    }

    @Test
    void calculate_missingMandatoryEliminatoryAnswer_shouldRecommendNoGo() {
        QuestionIndustrialisation question = question(5L, "Depot Git disponible ?", TypeCritere.ELIMINATOIRE, TypeReponseIndustrialisation.BOOLEAN, null, true);
        mockBase(List.of(question), List.of(), List.of(livrable(TypeLivrable.LIEN_GIT, "https://github.com/esprit/demo", null)), Optional.of(evaluationWithNote(95.0)), List.of(livrableRule(TypeLivrable.LIEN_GIT)));

        var score = service.calculate(11L);

        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
        assertFalse(score.estBloqueParEliminatoire());
        assertTrue(score.elementsManquants().stream().anyMatch(item -> item.contains("Depot Git")));
    }

    @Test
    void calculate_invalidUrlQuestion_shouldScoreZeroAndMarkAnswered() {
        QuestionIndustrialisation url = question(2L, "URL", TypeCritere.NOTE, TypeReponseIndustrialisation.URL, 1.0, true);
        ReponseIndustrialisation urlResponse = response(url);
        urlResponse.setValeurUrl("not-a-valid-url");

        mockBase(List.of(url), List.of(urlResponse), List.of(), Optional.of(evaluationWithNote(80.0)), List.of());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreQuestions());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "NOTE_CALCULEE".equals(detail.statut())));
    }

    @Test
    void calculate_noteQuestions_shouldUseWeightedAverage() {
        QuestionIndustrialisation highWeight = question(1L, "Question forte", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 3.0, true);
        ReponseIndustrialisation highResponse = response(highWeight);
        highResponse.setValeurBoolean(true);
        QuestionIndustrialisation lowWeight = question(2L, "Question faible", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 1.0, true);
        ReponseIndustrialisation lowResponse = response(lowWeight);
        lowResponse.setValeurBoolean(false);

        mockBase(List.of(highWeight, lowWeight), List.of(highResponse, lowResponse), List.of(), Optional.empty(), List.of());

        var score = service.calculate(11L);

        assertEquals(75.0, score.scoreQuestions());
    }

    @Test
    void calculate_negativeQuestionWeight_shouldFallbackToWeightOne() {
        QuestionIndustrialisation good = question(1L, "Good", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, -5.0, true);
        ReponseIndustrialisation goodResponse = response(good);
        goodResponse.setValeurBoolean(true);
        QuestionIndustrialisation bad = question(2L, "Bad", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 1.0, true);
        ReponseIndustrialisation badResponse = response(bad);
        badResponse.setValeurBoolean(false);

        mockBase(List.of(good, bad), List.of(goodResponse, badResponse), List.of(), Optional.empty(), List.of());

        var score = service.calculate(11L);

        assertEquals(50.0, score.scoreQuestions());
    }

    @Test
    void calculate_noCriteriaAvailable_shouldNotReturnNaN() {
        mockBase(List.of(), List.of(), List.of(), Optional.empty(), List.of());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreFinal());
        assertEquals(0.0, score.scoreQuestions());
        assertEquals(0.0, score.scoreCriteresNotes());
    }

    @Test
    void calculate_noCriteriaButNonApplicableEvaluation_shouldReturnNoCriteriaResponse() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setResultats(null);
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluation), List.of());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreFinal());
        assertEquals("NO_CRITERIA", score.detailsCalcul().get(0).reference());
    }

    @Test
    void calculate_evaluationWithoutDetailedNotes_shouldUseGlobalEvaluationScore() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(72.5);
        ResultatCritere eliminatoryOk = new ResultatCritere();
        eliminatoryOk.setCritereId(8L);
        eliminatoryOk.setCritereLibelle("Legal");
        eliminatoryOk.setTypeCritere(TypeCritere.ELIMINATOIRE);
        eliminatoryOk.setRuleConfigured(true);
        eliminatoryOk.setReponseEliminatoire(ReponseEliminatoire.OK);
        evaluation.addResultat(eliminatoryOk);
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluation), List.of());

        var score = service.calculate(11L);

        assertEquals(72.5, score.scoreCriteresNotes());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "SCORE_GLOBAL".equals(detail.statut())));
    }

    @Test
    void calculate_evaluationNotesWithInvalidDetails_shouldIgnoreInvalidAndFallbackToGlobalScore() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(66.0);
        evaluation.addResultat(noteResult(1L, "Disabled", 5.0, 5.0, 1.0, false));
        evaluation.addResultat(noteResult(2L, "No note", null, null, 1.0, true));
        evaluation.addResultat(noteResult(3L, "No note value", null, 5.0, 1.0, true));
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluation), List.of(livrableRule(TypeLivrable.RAPPORT)));

        var score = service.calculate(11L);

        assertEquals(66.0, score.scoreCriteresNotes());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> "SCORE_GLOBAL".equals(detail.statut())));
    }

    @Test
    void calculate_evaluationNotesShouldClampAndUseEvidenceSummary() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        ResultatCritere aboveMax = noteResult(1L, "Over", 150.0, 100.0, 1.0, true);
        aboveMax.setEvidenceSummary("preuve fournie");
        ResultatCritere belowZero = noteResult(2L, "Negative", -10.0, 100.0, 1.0, true);
        evaluation.addResultat(aboveMax);
        evaluation.addResultat(belowZero);
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluation), List.of(livrableRule(TypeLivrable.RAPPORT)));

        var score = service.calculate(11L);

        assertEquals(50.0, score.scoreCriteresNotes());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> detail.details().equals("preuve fournie")));
    }

    @Test
    void calculate_evaluationNoteWithoutBareme_shouldUseActiveMaxNoteValue() {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        ResultatCritere note = noteResult(7L, "Score dynamique", 7.0, null, 1.0, true);
        note.setNoteValue(7);
        evaluation.addResultat(note);
        when(noteLevelService.maxActiveNoteValue()).thenReturn(10);
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluation), List.of());

        var score = service.calculate(11L);

        assertEquals(70.0, score.scoreCriteresNotes());
    }

    @Test
    void calculate_noLatestEvaluationButProjectSnapshot_shouldUseSnapshotAndExposeProjectWarning() {
        projet.setBloqueParEliminatoire(true);
        mockBase(List.of(), List.of(), List.of(livrable(TypeLivrable.RAPPORT, null, "rapport.pdf")),
                Optional.empty(), List.of(livrableRule(TypeLivrable.RAPPORT)));

        var score = service.calculate(11L);

        assertEquals(90.0, score.scoreCriteresNotes());
        assertFalse(score.estBloqueParEliminatoire());
        assertTrue(score.hasEliminatoryWarnings());
        assertEquals(DecisionRecommandeeIndustrialisation.GO, score.decisionRecommandee());
    }

    @Test
    void calculate_missingEvaluationComponent_shouldExcludeItFromFinalScore() {
        projet.setScoreFinalEvaluation(null);
        QuestionIndustrialisation question = question(1L, "Complet", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 1.0, true);
        ReponseIndustrialisation reponse = response(question);
        reponse.setValeurBoolean(true);
        mockBase(List.of(question), List.of(reponse), List.of(), Optional.empty(), List.of());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreCriteresNotes());
        assertEquals(57.14, score.scoreFinal());
        assertTrue(score.elementsManquants().contains("Evaluation projet absente."));
    }

    @Test
    void calculate_noLivrablesAvailable_shouldReturnCoherentMissingState() {
        Evaluation evaluation = evaluationWithNote(80.0);
        evaluation.addResultat(noteResult(1L, "Score projet", 4.0, 5.0, 1.0, true));
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluation), List.of());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreLivrables());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail ->
                "LIVRABLES".equals(detail.composant()) && "MANQUANT".equals(detail.statut())));
    }

    @Test
    void calculate_supportedAnswerTypes_shouldScoreDeterministically() {
        QuestionIndustrialisation bool = question(1L, "Boolean", TypeCritere.NOTE, TypeReponseIndustrialisation.BOOLEAN, 1.0, true);
        ReponseIndustrialisation boolResponse = response(bool);
        boolResponse.setValeurBoolean(true);
        QuestionIndustrialisation url = question(2L, "URL", TypeCritere.NOTE, TypeReponseIndustrialisation.URL, 1.0, true);
        ReponseIndustrialisation urlResponse = response(url);
        urlResponse.setValeurUrl("https://esprit.tn");
        QuestionIndustrialisation file = question(3L, "Fichier", TypeCritere.NOTE, TypeReponseIndustrialisation.FICHIER, 1.0, true);
        ReponseIndustrialisation fileResponse = response(file);
        fileResponse.setPreuveObjectName("preuves/1.pdf");
        QuestionIndustrialisation text = question(4L, "Texte", TypeCritere.NOTE, TypeReponseIndustrialisation.TEXTE, 1.0, true);
        ReponseIndustrialisation textResponse = response(text);
        textResponse.setValeurTexte("Complet");

        mockBase(List.of(bool, url, file, text), List.of(boolResponse, urlResponse, fileResponse, textResponse), List.of(), Optional.empty(), List.of());

        var score = service.calculate(11L);

        assertEquals(100.0, score.scoreQuestions());
        assertTrue(score.detailsCalcul().stream().anyMatch(detail -> Boolean.TRUE.equals(detail.revueManuelleRequise())));
    }

    private void mockBase(List<QuestionIndustrialisation> questions,
                          List<ReponseIndustrialisation> responses,
                          List<Livrable> livrables,
                          Optional<Evaluation> evaluation,
                          List<CritereNote> critereNotes) {
        mockBase(questions, responses, livrables, evaluation, critereNotes, List.of());
    }

    private void mockBase(List<QuestionIndustrialisation> questions,
                          List<ReponseIndustrialisation> responses,
                          List<Livrable> livrables,
                          Optional<Evaluation> evaluation,
                          List<CritereNote> critereNotes,
                          List<CritereEliminatoire> criteresEliminatoires) {
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(questions);
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(responses);
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L)).thenReturn(livrables);
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(42L)).thenReturn(evaluation);
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(critereNotes);
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(criteresEliminatoires);
    }

    private QuestionIndustrialisation question(Long id,
                                               String libelle,
                                               TypeCritere typeCritere,
                                               TypeReponseIndustrialisation typeReponse,
                                               Double poids,
                                               boolean obligatoire) {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setId(id);
        question.setLibelle(libelle);
        question.setTypeCritere(typeCritere);
        question.setTypeReponse(typeReponse);
        question.setPoids(poids);
        question.setObligatoire(obligatoire);
        question.setActif(true);
        question.setOrdre(id.intValue());
        return question;
    }

    private ReponseIndustrialisation response(QuestionIndustrialisation question) {
        ReponseIndustrialisation response = new ReponseIndustrialisation();
        response.setQuestion(question);
        response.setCandidatureIndustrialisation(candidature);
        return response;
    }

    private Livrable livrable(TypeLivrable type, String link, String fileName) {
        Livrable livrable = new Livrable();
        livrable.setId(100L + type.ordinal());
        livrable.setTypeLivrable(type);
        livrable.setNom(type.name());
        livrable.setLienExterne(link);
        livrable.setOriginalFileName(fileName);
        if (fileName != null) {
            livrable.setObjectName("livrables/" + fileName);
        }
        return livrable;
    }

    private CritereNote livrableRule(TypeLivrable type) {
        CritereNote critere = new CritereNote();
        critere.setId(200L + type.ordinal());
        critere.setLibelle("Livrable " + type.name());
        critere.setDomaine("livrables");
        critere.setActif(true);
        critere.setRuleEnabled(true);
        critere.setModeEvaluation(ModeEvaluation.LIVRABLE_TYPE_PRESENT);
        critere.setExpectedLivrableTypes(type.name());
        return critere;
    }

    private CritereEliminatoire livrableEliminatoryRule(TypeLivrable type) {
        CritereEliminatoire critere = new CritereEliminatoire();
        critere.setId(400L + type.ordinal());
        critere.setLibelle("Livrable eliminatoire " + type.name());
        critere.setDomaine("livrables");
        critere.setActif(true);
        critere.setRuleEnabled(true);
        critere.setModeEvaluation(ModeEvaluation.LIVRABLE_TYPE_PRESENT);
        critere.setExpectedLivrableTypes(type.name());
        critere.setReponseAttendue(ReponseEliminatoire.OK);
        return critere;
    }

    private CritereNote ruleWithMode(ModeEvaluation mode, String expectedTypes) {
        CritereNote critere = new CritereNote();
        critere.setId(500L + mode.ordinal());
        critere.setLibelle("Rule " + mode.name());
        critere.setDomaine("livrables");
        critere.setActif(true);
        critere.setRuleEnabled(true);
        critere.setModeEvaluation(mode);
        critere.setExpectedLivrableTypes(expectedTypes);
        return critere;
    }

    private ResultatCritere noteResult(Long id,
                                       String label,
                                       Double note,
                                       Double bareme,
                                       Double poids,
                                       boolean ruleConfigured) {
        ResultatCritere resultat = new ResultatCritere();
        resultat.setCritereId(id);
        resultat.setCritereLibelle(label);
        resultat.setTypeCritere(TypeCritere.NOTE);
        resultat.setRuleConfigured(ruleConfigured);
        resultat.setNoteObtenue(note);
        resultat.setBareme(bareme);
        resultat.setPoids(poids);
        return resultat;
    }

    private Evaluation evaluationWithNote(double normalizedScore) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(normalizedScore);
        evaluation.setEligibleIndustrialisation(true);
        evaluation.setBloqueParEliminatoire(false);
        return evaluation;
    }
}
