package tn.esprit.backend_esprittech.service.industrialisation;

import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;

import java.net.URI;

final class IndustrialisationQuestionEvaluator {

    private IndustrialisationQuestionEvaluator() {
    }

    static boolean isAnswered(QuestionIndustrialisation question, ReponseIndustrialisation reponse) {
        if (question == null || reponse == null) {
            return false;
        }
        return switch (question.getTypeReponse()) {
            case BOOLEAN -> reponse.getValeurBoolean() != null;
            case NUMERIQUE -> reponse.getValeurNumerique() != null;
            case URL -> StringUtils.hasText(reponse.getValeurUrl());
            case FICHIER -> hasProof(reponse);
            case TEXTE, CHOIX -> StringUtils.hasText(reponse.getValeurTexte());
        };
    }

    static ReponseEliminatoire eliminatoryResult(QuestionIndustrialisation question, ReponseIndustrialisation reponse) {
        if (question == null || question.getTypeCritere() != TypeCritere.ELIMINATOIRE) {
            return null;
        }
        if (!isAnswered(question, reponse)) {
            return Boolean.TRUE.equals(question.getObligatoire()) ? ReponseEliminatoire.NOT_OK : null;
        }
        if (question.getTypeReponse() == TypeReponseIndustrialisation.BOOLEAN) {
            return Boolean.TRUE.equals(reponse.getValeurBoolean())
                    ? ReponseEliminatoire.OK
                    : ReponseEliminatoire.NOT_OK;
        }
        return ReponseEliminatoire.OK;
    }

    static double scoreQuestion(QuestionIndustrialisation question, ReponseIndustrialisation reponse) {
        if (!isAnswered(question, reponse)) {
            return 0.0;
        }
        return switch (question.getTypeReponse()) {
            case BOOLEAN -> Boolean.TRUE.equals(reponse.getValeurBoolean()) ? 100.0 : 0.0;
            case URL -> isValidUrl(reponse.getValeurUrl()) ? 100.0 : 0.0;
            case FICHIER -> hasProof(reponse) ? 100.0 : 0.0;
            case TEXTE, CHOIX, NUMERIQUE -> 100.0;
        };
    }

    static boolean requiresManualReview(QuestionIndustrialisation question, ReponseIndustrialisation reponse) {
        if (!isAnswered(question, reponse)) {
            return false;
        }
        return switch (question.getTypeReponse()) {
            case TEXTE, CHOIX, NUMERIQUE -> true;
            case BOOLEAN, URL, FICHIER -> false;
        };
    }

    static boolean hasProof(ReponseIndustrialisation reponse) {
        return reponse != null
                && (StringUtils.hasText(reponse.getPreuveObjectName())
                || StringUtils.hasText(reponse.getPreuveOriginalFileName()));
    }

    static boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && StringUtils.hasText(uri.getHost());
        } catch (Exception ex) {
            return false;
        }
    }
}
