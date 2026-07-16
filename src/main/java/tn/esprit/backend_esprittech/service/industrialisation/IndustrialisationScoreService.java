package tn.esprit.backend_esprittech.service.industrialisation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.dto.industrialisation.BlocageEliminatoireDto;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionRecommandeeIndustrialisation;
import tn.esprit.backend_esprittech.dto.industrialisation.IndustrialisationScoreResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.ScoreBreakdownDto;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.service.CritereEval.NoteLevelService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndustrialisationScoreService {

    private static final double QUESTIONS_WEIGHT = 0.0;
    private static final double LIVRABLES_WEIGHT = 0.0;
    private static final double CRITERES_NOTES_WEIGHT = 1.0;

    private final CandidatureIndustrialisationRepository candidatureRepository;
    private final QuestionIndustrialisationRepository questionRepository;
    private final ReponseIndustrialisationRepository reponseRepository;
    private final EvaluationRepository evaluationRepository;
    private final NoteLevelService noteLevelService;

    public IndustrialisationScoreResponse calculate(Long candidatureId) {
        CandidatureIndustrialisation candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande industrialisation introuvable avec id: " + candidatureId));

        List<QuestionIndustrialisation> questions = questionRepository.findByActifTrueOrderByOrdreAsc();
        Map<Long, ReponseIndustrialisation> reponsesByQuestion = reponseRepository
                .findByCandidatureIndustrialisationId(candidatureId)
                .stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), Function.identity()));
        Optional<Evaluation> latestEvaluation = evaluationRepository
                .findTopBySujetProjetIdOrderByDateCalculDesc(candidature.getSujetProjet().getId());

        if (noCriteriaAvailable(questions, latestEvaluation, candidature.getSujetProjet().getScoreFinalEvaluation())) {
            return emptyCriteriaResponse(candidature);
        }

        List<ScoreBreakdownDto> details = new ArrayList<>();
        List<BlocageEliminatoireDto> blocages = new ArrayList<>();
        List<String> elementsManquants = new ArrayList<>();

        ComponentScore questionScore = calculateQuestionScore(questions, reponsesByQuestion, details, blocages, elementsManquants);
        ComponentScore livrableScore = calculateLivrableScore(details);
        ComponentScore critereScore = calculateCritereNoteScore(candidature, latestEvaluation, details, blocages, elementsManquants);

        double scoreFinal = calculateWeightedFinalScore(List.of(
                new WeightedComponent(QUESTIONS_WEIGHT, questionScore),
                new WeightedComponent(LIVRABLES_WEIGHT, livrableScore),
                new WeightedComponent(CRITERES_NOTES_WEIGHT, critereScore)
        ));
        boolean hasWarnings = !blocages.isEmpty();
        DecisionRecommandeeIndustrialisation decision = recommend(scoreFinal);

        return new IndustrialisationScoreResponse(
                candidature.getId(),
                candidature.getSujetProjet().getId(),
                scoreFinal,
                decision,
                false,
                blocages,
                hasWarnings,
                blocages.size(),
                blocages,
                questionScore.score(),
                livrableScore.score(),
                critereScore.score(),
                details,
                elementsManquants,
                recommendationMessage(decision, scoreFinal, blocages)
        );
    }

    private boolean noCriteriaAvailable(List<QuestionIndustrialisation> questions,
                                        Optional<Evaluation> latestEvaluation,
                                        Double projectScoreSnapshot) {
        return questions.isEmpty()
                && projectScoreSnapshot == null
                && latestEvaluation.filter(this::hasApplicableEvaluationData).isEmpty();
    }

    private boolean hasApplicableEvaluationData(Evaluation evaluation) {
        if (evaluation.getResultats() == null) {
            return false;
        }
        return evaluation.getResultats().stream().anyMatch(this::isApplicableEvaluationResult);
    }

    private boolean isApplicableEvaluationResult(ResultatCritere resultat) {
        if (resultat == null
                || resultat.getTypeCritere() == null
                || !Boolean.TRUE.equals(resultat.getRuleConfigured())) {
            return false;
        }
        if (resultat.getTypeCritere() == TypeCritere.ELIMINATOIRE) {
            return resultat.getReponseEliminatoire() != null;
        }
        return resultat.getTypeCritere() == TypeCritere.NOTE
                && (resultat.getNoteValue() != null || resultat.getNoteObtenue() != null);
    }

    private IndustrialisationScoreResponse emptyCriteriaResponse(CandidatureIndustrialisation candidature) {
        return new IndustrialisationScoreResponse(
                candidature.getId(),
                candidature.getSujetProjet().getId(),
                0.0,
                DecisionRecommandeeIndustrialisation.NO_GO,
                false,
                List.of(),
                false,
                0,
                List.of(),
                0.0,
                0.0,
                0.0,
                List.of(new ScoreBreakdownDto(
                        "GLOBAL",
                        "NO_CRITERIA",
                        "Calcul industrialisation",
                        0.0,
                        null,
                        "NON_EVALUABLE",
                        "Aucun critere actif ni resultat d'evaluation applicable n'est disponible; score force a 0.",
                        true
                )),
                List.of("Aucun critere actif ni resultat d'evaluation applicable n'est disponible."),
                "NO GO recommande : aucun critere disponible pour calculer un score."
        );
    }

    private ComponentScore calculateQuestionScore(List<QuestionIndustrialisation> questions,
                                                  Map<Long, ReponseIndustrialisation> reponsesByQuestion,
                                                  List<ScoreBreakdownDto> details,
                                                  List<BlocageEliminatoireDto> blocages,
                                                  List<String> elementsManquants) {
        double weightedScores = 0.0;
        double totalWeight = 0.0;

        for (QuestionIndustrialisation question : questions) {
            ReponseIndustrialisation reponse = reponsesByQuestion.get(question.getId());
            if (question.getTypeCritere() == TypeCritere.ELIMINATOIRE) {
                ReponseEliminatoire result = IndustrialisationQuestionEvaluator.eliminatoryResult(question, reponse);
                boolean answered = IndustrialisationQuestionEvaluator.isAnswered(question, reponse);
                if (result == ReponseEliminatoire.NOT_OK && (Boolean.TRUE.equals(question.getObligatoire()) || answered)) {
                    blocages.add(new BlocageEliminatoireDto(
                            "QUESTIONNAIRE",
                            question.getId(),
                            question.getLibelle(),
                            answered
                                    ? "Reponse non conforme pour une question eliminatoire."
                                    : "Question eliminatoire obligatoire sans reponse."
                    ));
                }
                if (Boolean.TRUE.equals(question.getObligatoire()) && !answered) {
                    elementsManquants.add("Question obligatoire manquante: " + question.getLibelle());
                }
                details.add(new ScoreBreakdownDto(
                        "QUESTIONNAIRE_ELIMINATOIRE",
                        String.valueOf(question.getId()),
                        question.getLibelle(),
                        result == null ? null : (result == ReponseEliminatoire.OK ? 100.0 : 0.0),
                        null,
                        result == null ? "EN_ATTENTE" : result.name(),
                        "Critere eliminatoire calcule automatiquement.",
                        false
                ));
                continue;
            }

            double weight = positiveWeight(question.getPoids());
            double normalizedScore = IndustrialisationQuestionEvaluator.scoreQuestion(question, reponsesByQuestion.get(question.getId()));
            boolean answered = IndustrialisationQuestionEvaluator.isAnswered(question, reponse);
            boolean manualReview = IndustrialisationQuestionEvaluator.requiresManualReview(question, reponse);
            weightedScores += normalizedScore * weight;
            totalWeight += weight;
            if (Boolean.TRUE.equals(question.getObligatoire()) && !answered) {
                elementsManquants.add("Question obligatoire manquante: " + question.getLibelle());
            }
            details.add(new ScoreBreakdownDto(
                    "QUESTIONNAIRE_NOTE",
                    String.valueOf(question.getId()),
                    question.getLibelle(),
                    round(normalizedScore),
                    weight,
                    answered ? "NOTE_CALCULEE" : "MANQUANT",
                    manualReview
                            ? "Score de completude automatique; la qualite doit etre revue par la CI."
                            : "Score calcule depuis la reponse fournie.",
                    manualReview
            ));
        }

        return totalWeight > 0.0
                ? new ComponentScore(round(weightedScores / totalWeight), true)
                : new ComponentScore(0.0, false);
    }

    private ComponentScore calculateLivrableScore(List<ScoreBreakdownDto> details) {
        details.add(new ScoreBreakdownDto(
                "LIVRABLES",
                "ML_CONTENT",
                "Livrables projet",
                null,
                LIVRABLES_WEIGHT,
                "COUVERT_PAR_EVALUATION_ML",
                "Les livrables sont notes par l'evaluation ML de contenu, pas par leur presence, type, nom, taille ou lien.",
                false
        ));
        return new ComponentScore(0.0, false);
    }

    private ComponentScore calculateCritereNoteScore(CandidatureIndustrialisation candidature,
                                                     Optional<Evaluation> latestEvaluation,
                                                     List<ScoreBreakdownDto> details,
                                                     List<BlocageEliminatoireDto> blocages,
                                                     List<String> elementsManquants) {
        if (latestEvaluation.isEmpty()) {
            Double snapshot = candidature.getSujetProjet().getScoreFinalEvaluation();
            if (snapshot != null) {
                details.add(new ScoreBreakdownDto(
                        "CRITERES_NOTES",
                        "SNAPSHOT",
                        "Score evaluation projet",
                        round(snapshot),
                        CRITERES_NOTES_WEIGHT,
                        "SNAPSHOT_PROJET",
                        "Aucune evaluation detaillee trouvee; utilisation du score projet sauvegarde.",
                        true
                ));
                if (Boolean.TRUE.equals(candidature.getSujetProjet().getBloqueParEliminatoire())) {
                    blocages.add(new BlocageEliminatoireDto(
                            "EVALUATION_PROJET",
                            candidature.getSujetProjet().getId(),
                            "Critere eliminatoire projet",
                            "Le projet est marque bloque par un critere eliminatoire."
                    ));
                }
                return new ComponentScore(round(snapshot), true);
            }
            elementsManquants.add("Evaluation projet absente.");
            details.add(new ScoreBreakdownDto(
                    "CRITERES_NOTES",
                    "EVALUATION",
                    "Evaluation projet",
                    0.0,
                    CRITERES_NOTES_WEIGHT,
                    "NON_EVALUE",
                    "Aucun resultat de criteres notes n'est disponible; composant exclu du score final.",
                    true
            ));
            return new ComponentScore(0.0, false);
        }

        Evaluation evaluation = latestEvaluation.get();
        if (evaluation.getFinalValidatedScore() != null) {
            details.add(new ScoreBreakdownDto(
                    "CRITERES_NOTES",
                    String.valueOf(evaluation.getId()),
                    "Score final valide",
                    round(evaluation.getFinalValidatedScore()),
                    CRITERES_NOTES_WEIGHT,
                    "SCORE_VALIDE",
                    "Score final valide ou surcharge par un utilisateur autorise.",
                    false
            ));
            return new ComponentScore(round(evaluation.getFinalValidatedScore()), true);
        }
        double weightedScores = 0.0;
        double totalWeight = 0.0;
        for (ResultatCritere resultat : evaluation.getResultats()) {
            if (resultat.getTypeCritere() == TypeCritere.ELIMINATOIRE
                    && resultat.getReponseEliminatoire() == ReponseEliminatoire.NOT_OK) {
                blocages.add(new BlocageEliminatoireDto(
                        "EVALUATION_PROJET",
                        resultat.getCritereId(),
                        resultat.getCritereLibelle(),
                        "Critere eliminatoire projet NOT_OK."
                ));
            }
            if (resultat.getTypeCritere() != TypeCritere.NOTE || !Boolean.TRUE.equals(resultat.getRuleConfigured())) {
                continue;
            }
            if (resultat.getNoteValue() == null && resultat.getNoteObtenue() == null) {
                continue;
            }
            double weight = positiveWeight(resultat.getPoids());
            double normalized = resultat.getNormalizedScore() != null
                    ? clamp(resultat.getNormalizedScore() * 100.0)
                    : clamp(((resultat.getNoteValue() != null ? (double) resultat.getNoteValue() : resultat.getNoteObtenue())
                    / noteScale(resultat)) * 100.0);
            weightedScores += normalized * weight;
            totalWeight += weight;
            details.add(new ScoreBreakdownDto(
                    "CRITERES_NOTES",
                    String.valueOf(resultat.getCritereId()),
                    resultat.getCritereLibelle(),
                    round(normalized),
                    weight,
                    "NOTE_PROJET",
                    StringUtils.hasText(resultat.getEvidenceSummary())
                            ? resultat.getEvidenceSummary()
                            : "Resultat note issu de l'evaluation projet.",
                    false
            ));
        }

        if (totalWeight > 0.0) {
            return new ComponentScore(round(weightedScores / totalWeight), true);
        }
        details.add(new ScoreBreakdownDto(
                "CRITERES_NOTES",
                String.valueOf(evaluation.getId()),
                "Evaluation projet",
                round(nullToZero(evaluation.getScoreFinal())),
                CRITERES_NOTES_WEIGHT,
                "SCORE_GLOBAL",
                "Aucun detail de critere note configure; utilisation du score global de l'evaluation.",
                true
        ));
        return evaluation.getScoreFinal() == null
                ? new ComponentScore(0.0, false)
                : new ComponentScore(round(evaluation.getScoreFinal()), true);
    }

    private double calculateWeightedFinalScore(List<WeightedComponent> weightedComponents) {
        double weightedScores = 0.0;
        double totalWeights = 0.0;
        for (WeightedComponent component : weightedComponents) {
            if (!component.score().applicable()) {
                continue;
            }
            weightedScores += component.score().score() * component.weight();
            totalWeights += component.weight();
        }
        return totalWeights > 0.0 ? round(weightedScores / totalWeights) : 0.0;
    }

    private DecisionRecommandeeIndustrialisation recommend(double scoreFinal) {
        if (scoreFinal >= 80.0) {
            return DecisionRecommandeeIndustrialisation.GO;
        }
        if (scoreFinal >= 60.0) {
            return DecisionRecommandeeIndustrialisation.A_INSTRUIRE;
        }
        return DecisionRecommandeeIndustrialisation.NO_GO;
    }

    private String recommendationMessage(DecisionRecommandeeIndustrialisation decision,
                                         double scoreFinal,
        List<BlocageEliminatoireDto> blocages) {
        if (!blocages.isEmpty()) {
            return "Alerte eliminatoire non bloquante : " + blocages.size()
                    + " critere(s) a examiner par la CI. Decision finale manuelle.";
        }
        return switch (decision) {
            case GO -> "GO recommande : score superieur au seuil, decision finale CI requise.";
            case A_INSTRUIRE -> "A instruire : score intermediaire, verification CI requise.";
            case NO_GO -> "NO GO recommande : score inferieur au seuil minimal.";
        };
    }

    private double positiveWeight(Double value) {
        return value == null || value <= 0.0 ? 1.0 : value;
    }

    private double noteScale(ResultatCritere resultat) {
        if (resultat.getBareme() != null && resultat.getBareme() > 0) {
            return resultat.getBareme();
        }
        return noteLevelService.maxActiveNoteValue();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ComponentScore(Double score, boolean applicable) {
    }

    private record WeightedComponent(double weight, ComponentScore score) {
    }
}
