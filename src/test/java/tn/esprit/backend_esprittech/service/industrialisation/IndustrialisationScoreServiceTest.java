package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionRecommandeeIndustrialisation;
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
    void calculate_eliminatoryNotOkWithHighNumericScore_shouldRecommendNoGo() {
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
        assertEquals(DecisionRecommandeeIndustrialisation.NO_GO, score.decisionRecommandee());
        assertTrue(score.estBloqueParEliminatoire());
        assertEquals("Depot Git disponible ?", score.blocagesEliminatoires().get(0).libelle());
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
    void calculate_missingMandatoryEliminatoryAnswer_shouldRecommendNoGo() {
        QuestionIndustrialisation question = question(5L, "Depot Git disponible ?", TypeCritere.ELIMINATOIRE, TypeReponseIndustrialisation.BOOLEAN, null, true);
        mockBase(List.of(question), List.of(), List.of(livrable(TypeLivrable.LIEN_GIT, "https://github.com/esprit/demo", null)), Optional.of(evaluationWithNote(95.0)), List.of(livrableRule(TypeLivrable.LIEN_GIT)));

        var score = service.calculate(11L);

        assertEquals(DecisionRecommandeeIndustrialisation.NO_GO, score.decisionRecommandee());
        assertTrue(score.estBloqueParEliminatoire());
        assertTrue(score.elementsManquants().stream().anyMatch(item -> item.contains("Depot Git")));
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
    void calculate_noCriteriaAvailable_shouldNotReturnNaN() {
        mockBase(List.of(), List.of(), List.of(), Optional.empty(), List.of());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreFinal());
        assertEquals(0.0, score.scoreQuestions());
        assertEquals(0.0, score.scoreCriteresNotes());
    }

    @Test
    void calculate_noLivrablesAvailable_shouldReturnCoherentMissingState() {
        mockBase(List.of(), List.of(), List.of(), Optional.of(evaluationWithNote(80.0)), List.of());

        var score = service.calculate(11L);

        assertEquals(0.0, score.scoreLivrables());
        assertTrue(score.elementsManquants().stream().anyMatch(item -> item.contains("Aucun livrable")));
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
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(questions);
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(responses);
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L)).thenReturn(livrables);
        when(evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(42L)).thenReturn(evaluation);
        when(critereNoteRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(critereNotes);
        when(critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of());
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

    private Evaluation evaluationWithNote(double normalizedScore) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(30L);
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(normalizedScore);
        evaluation.setEligibleIndustrialisation(true);
        evaluation.setBloqueParEliminatoire(false);

        ResultatCritere resultat = new ResultatCritere();
        resultat.setCritereId(300L);
        resultat.setCritereLibelle("Evaluation projet");
        resultat.setTypeCritere(TypeCritere.NOTE);
        resultat.setRuleConfigured(true);
        resultat.setNoteObtenue(normalizedScore);
        resultat.setBareme(100.0);
        resultat.setPoids(1.0);
        evaluation.addResultat(resultat);
        return evaluation;
    }
}
