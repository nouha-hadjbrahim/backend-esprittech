package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndustrialisationQuestionEvaluatorTest {

    @Test
    void isAnswered_shouldReturnFalseForNullQuestionOrResponse() {
        assertFalse(IndustrialisationQuestionEvaluator.isAnswered(null, new ReponseIndustrialisation()));
        assertFalse(IndustrialisationQuestionEvaluator.isAnswered(question(TypeReponseIndustrialisation.TEXTE), null));
    }

    @Test
    void isAnswered_shouldEvaluateAllSupportedResponseTypes() {
        ReponseIndustrialisation booleanAnswer = new ReponseIndustrialisation();
        booleanAnswer.setValeurBoolean(false);
        assertTrue(IndustrialisationQuestionEvaluator.isAnswered(question(TypeReponseIndustrialisation.BOOLEAN), booleanAnswer));

        ReponseIndustrialisation numericAnswer = new ReponseIndustrialisation();
        numericAnswer.setValeurNumerique(12.5);
        assertTrue(IndustrialisationQuestionEvaluator.isAnswered(question(TypeReponseIndustrialisation.NUMERIQUE), numericAnswer));

        ReponseIndustrialisation urlAnswer = new ReponseIndustrialisation();
        urlAnswer.setValeurUrl("https://git.example.com/project");
        assertTrue(IndustrialisationQuestionEvaluator.isAnswered(question(TypeReponseIndustrialisation.URL), urlAnswer));

        ReponseIndustrialisation fileAnswer = new ReponseIndustrialisation();
        fileAnswer.setPreuveOriginalFileName("rapport.pdf");
        assertTrue(IndustrialisationQuestionEvaluator.isAnswered(question(TypeReponseIndustrialisation.FICHIER), fileAnswer));

        ReponseIndustrialisation textAnswer = new ReponseIndustrialisation();
        textAnswer.setValeurTexte("Preuve fournie");
        assertTrue(IndustrialisationQuestionEvaluator.isAnswered(question(TypeReponseIndustrialisation.TEXTE), textAnswer));
        assertTrue(IndustrialisationQuestionEvaluator.isAnswered(question(TypeReponseIndustrialisation.CHOIX), textAnswer));
    }

    @Test
    void eliminatoryResult_shouldReturnTechnicalResultFromBooleanAnswer() {
        QuestionIndustrialisation question = question(TypeReponseIndustrialisation.BOOLEAN);
        question.setTypeCritere(TypeCritere.ELIMINATOIRE);
        question.setObligatoire(true);

        ReponseIndustrialisation ok = new ReponseIndustrialisation();
        ok.setValeurBoolean(true);
        assertEquals(ReponseEliminatoire.OK, IndustrialisationQuestionEvaluator.eliminatoryResult(question, ok));

        ReponseIndustrialisation notOk = new ReponseIndustrialisation();
        notOk.setValeurBoolean(false);
        assertEquals(ReponseEliminatoire.NOT_OK, IndustrialisationQuestionEvaluator.eliminatoryResult(question, notOk));
    }

    @Test
    void eliminatoryResult_shouldHandleMissingAndOptionalAnswers() {
        QuestionIndustrialisation mandatory = question(TypeReponseIndustrialisation.URL);
        mandatory.setTypeCritere(TypeCritere.ELIMINATOIRE);
        mandatory.setObligatoire(true);

        assertEquals(ReponseEliminatoire.NOT_OK,
                IndustrialisationQuestionEvaluator.eliminatoryResult(mandatory, new ReponseIndustrialisation()));

        QuestionIndustrialisation optional = question(TypeReponseIndustrialisation.URL);
        optional.setTypeCritere(TypeCritere.ELIMINATOIRE);
        optional.setObligatoire(false);

        assertNull(IndustrialisationQuestionEvaluator.eliminatoryResult(optional, new ReponseIndustrialisation()));
        assertNull(IndustrialisationQuestionEvaluator.eliminatoryResult(question(TypeReponseIndustrialisation.URL), new ReponseIndustrialisation()));
    }

    @Test
    void scoreQuestion_shouldScoreAutomaticTypesDeterministically() {
        ReponseIndustrialisation trueBoolean = new ReponseIndustrialisation();
        trueBoolean.setValeurBoolean(true);
        assertEquals(100.0, IndustrialisationQuestionEvaluator.scoreQuestion(question(TypeReponseIndustrialisation.BOOLEAN), trueBoolean));

        ReponseIndustrialisation falseBoolean = new ReponseIndustrialisation();
        falseBoolean.setValeurBoolean(false);
        assertEquals(0.0, IndustrialisationQuestionEvaluator.scoreQuestion(question(TypeReponseIndustrialisation.BOOLEAN), falseBoolean));

        ReponseIndustrialisation validUrl = new ReponseIndustrialisation();
        validUrl.setValeurUrl("https://esprit.tn");
        assertEquals(100.0, IndustrialisationQuestionEvaluator.scoreQuestion(question(TypeReponseIndustrialisation.URL), validUrl));

        ReponseIndustrialisation invalidUrl = new ReponseIndustrialisation();
        invalidUrl.setValeurUrl("ftp://esprit.tn");
        assertEquals(0.0, IndustrialisationQuestionEvaluator.scoreQuestion(question(TypeReponseIndustrialisation.URL), invalidUrl));

        ReponseIndustrialisation text = new ReponseIndustrialisation();
        text.setValeurTexte("Document disponible");
        assertEquals(100.0, IndustrialisationQuestionEvaluator.scoreQuestion(question(TypeReponseIndustrialisation.TEXTE), text));
    }

    @Test
    void requiresManualReview_shouldOnlyFlagScorableManualTypesWhenAnswered() {
        ReponseIndustrialisation text = new ReponseIndustrialisation();
        text.setValeurTexte("Analyse libre");
        assertTrue(IndustrialisationQuestionEvaluator.requiresManualReview(question(TypeReponseIndustrialisation.TEXTE), text));

        ReponseIndustrialisation numeric = new ReponseIndustrialisation();
        numeric.setValeurNumerique(4.0);
        assertTrue(IndustrialisationQuestionEvaluator.requiresManualReview(question(TypeReponseIndustrialisation.NUMERIQUE), numeric));

        ReponseIndustrialisation bool = new ReponseIndustrialisation();
        bool.setValeurBoolean(true);
        assertFalse(IndustrialisationQuestionEvaluator.requiresManualReview(question(TypeReponseIndustrialisation.BOOLEAN), bool));

        assertFalse(IndustrialisationQuestionEvaluator.requiresManualReview(question(TypeReponseIndustrialisation.TEXTE), new ReponseIndustrialisation()));
    }

    @Test
    void isValidUrl_shouldRejectBlankMalformedOrUnsupportedUrls() {
        assertFalse(IndustrialisationQuestionEvaluator.isValidUrl(null));
        assertFalse(IndustrialisationQuestionEvaluator.isValidUrl(" "));
        assertFalse(IndustrialisationQuestionEvaluator.isValidUrl("not a url"));
        assertFalse(IndustrialisationQuestionEvaluator.isValidUrl("mailto:test@esprit.tn"));
        assertFalse(IndustrialisationQuestionEvaluator.isValidUrl("https:///missing-host"));
        assertTrue(IndustrialisationQuestionEvaluator.isValidUrl(" http://example.com/path "));
    }

    private QuestionIndustrialisation question(TypeReponseIndustrialisation typeReponse) {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setTypeReponse(typeReponse);
        question.setTypeCritere(TypeCritere.NOTE);
        question.setObligatoire(true);
        return question;
    }
}
