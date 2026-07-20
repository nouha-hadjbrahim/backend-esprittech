package tn.esprit.backend_esprittech.service.industrialisation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.dto.industrialisation.BlocageEliminatoireDto;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionRecommandeeIndustrialisation;
import tn.esprit.backend_esprittech.dto.industrialisation.IndustrialisationScoreResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.ScoreBreakdownDto;
import tn.esprit.backend_esprittech.entity.critereeval.Evaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
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
    private static final String CATEGORIE_CRITERES_NOTES = "CRITERES_NOTES";


    private static final double QUESTIONS_WEIGHT = 0.0;
    private static final double LIVRABLES_WEIGHT = 0.0;
    private static final double CRITERES_NOTES_WEIGHT = 1.0;

    private final CandidatureIndustrialisationRepository candidatureRepository;
    private final QuestionIndustrialisationRepository questionRepository;
    private final ReponseIndustrialisationRepository reponseRepository;
    private final EvaluationRepository evaluationRepository;
    private final ProjetCatalogueRepository projetCatalogueRepository;
    private final SujetProjetRepository sujetProjetRepository;
    private final NoteLevelService noteLevelService;

    public IndustrialisationScoreResponse calculate(Long candidatureId) {
        CandidatureIndustrialisation candidature = candidatureRepository.findById(candidatureId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande industrialisation introuvable avec id: " + candidatureId));

        EvaluationProjectContext project = resolveEvaluationProject(candidature);

        List<QuestionIndustrialisation> questions = questionRepository.findByActifTrueOrderByOrdreAsc();
        Map<Long, ReponseIndustrialisation> reponsesByQuestion = reponseRepository
                .findByCandidatureIndustrialisationId(candidatureId)
                .stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), Function.identity()));
        Optional<Evaluation> latestEvaluation = project.latestEvaluation();

        if (noCriteriaAvailable(questions, latestEvaluation, project.scoreSnapshot())) {
            return emptyCriteriaResponse(candidature, project.displayProjetId());
        }

        List<ScoreBreakdownDto> details = new ArrayList<>();
        List<BlocageEliminatoireDto> blocages = new ArrayList<>();
        List<String> elementsManquants = new ArrayList<>();

        ComponentScore questionScore = calculateQuestionScore(questions, reponsesByQuestion, details, blocages, elementsManquants);
        ComponentScore livrableScore = calculateLivrableScore(details);
        ComponentScore critereScore = calculateCritereNoteScore(project, latestEvaluation, details, blocages, elementsManquants);

        double scoreFinal = calculateWeightedFinalScore(List.of(
                new WeightedComponent(QUESTIONS_WEIGHT, questionScore),
                new WeightedComponent(LIVRABLES_WEIGHT, livrableScore),
                new WeightedComponent(CRITERES_NOTES_WEIGHT, critereScore)
        ));
        boolean hasWarnings = !blocages.isEmpty();
        DecisionRecommandeeIndustrialisation decision = recommend(scoreFinal);

        return new IndustrialisationScoreResponse(
                candidature.getId(),
                project.displayProjetId(),
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
                recommendationMessage(decision, blocages)
        );
    }

    /**
     * Résout le projet d'évaluation (SujetProjet ou ProjetCatalogue) associé à la candidature.
     * Les deux entités ne sont pas interchangeables : on lit l'évaluation sur la bonne clé.
     */
    EvaluationProjectContext resolveEvaluationProject(CandidatureIndustrialisation candidature) {
        ProjetCatalogue projetCatalogue = candidature.getProjetCatalogue();
        SujetProjet sujetProjet = candidature.getSujetProjet();

        if (projetCatalogue != null) {
            Optional<Evaluation> catalogueEval = evaluationRepository
                    .findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(projetCatalogue.getId());
            Optional<Evaluation> latest = catalogueEval.isPresent()
                    ? catalogueEval
                    : Optional.ofNullable(projetCatalogue.getSujetId())
                            .flatMap(evaluationRepository::findTopBySujetProjetIdOrderByDateCalculDescIdDesc);

            Double snapshot = resolveCatalogueScoreSnapshot(projetCatalogue);
            Boolean bloque = latest.map(Evaluation::getBloqueParEliminatoire)
                    .or(() -> resolveSujetBloqueFlag(projetCatalogue.getSujetId()))
                    .orElse(null);
            return new EvaluationProjectContext(
                    projetCatalogue.getId(),
                    latest,
                    snapshot,
                    bloque,
                    projetCatalogue.getId()
            );
        }

        if (sujetProjet != null) {
            Optional<Evaluation> sujetEval = evaluationRepository
                    .findTopBySujetProjetIdOrderByDateCalculDescIdDesc(sujetProjet.getId());
            Optional<Evaluation> latest = sujetEval.isPresent()
                    ? sujetEval
                    : projetCatalogueRepository.findBySujetId(sujetProjet.getId())
                            .flatMap(pc -> evaluationRepository
                                    .findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(pc.getId()));

            Double snapshot = sujetProjet.getScoreFinalEvaluation();
            if (snapshot == null) {
                snapshot = projetCatalogueRepository.findBySujetId(sujetProjet.getId())
                        .map(this::resolveCatalogueScoreSnapshot)
                        .orElse(null);
            }

            return new EvaluationProjectContext(
                    sujetProjet.getId(),
                    latest,
                    snapshot,
                    sujetProjet.getBloqueParEliminatoire(),
                    sujetProjet.getId()
            );
        }

        throw new BusinessException(
                "La demande d'industrialisation n'est associee ni a un sujet ni a un projet catalogue.");
    }

    private Double resolveCatalogueScoreSnapshot(ProjetCatalogue projetCatalogue) {
        if (projetCatalogue.getScore() != null && projetCatalogue.getScore() > 0) {
            return projetCatalogue.getScore().doubleValue();
        }
        return resolveSujetScoreSnapshot(projetCatalogue.getSujetId());
    }

    private Double resolveSujetScoreSnapshot(Long sujetId) {
        if (sujetId == null) {
            return null;
        }
        return sujetProjetRepository.findById(sujetId)
                .map(SujetProjet::getScoreFinalEvaluation)
                .orElse(null);
    }

    /**
     * Drapeau "bloque par un critere eliminatoire" du sujet d'origine. Vide lorsque l'information
     * est inconnue : absence de sujet, sujet introuvable, ou drapeau non renseigne. Cet etat
     * indetermine est distinct de {@code false} et doit le rester jusqu'a la reponse HTTP.
     */
    private Optional<Boolean> resolveSujetBloqueFlag(Long sujetId) {
        if (sujetId == null) {
            return Optional.empty();
        }
        return sujetProjetRepository.findById(sujetId)
                .map(SujetProjet::getBloqueParEliminatoire);
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

    private IndustrialisationScoreResponse emptyCriteriaResponse(CandidatureIndustrialisation candidature,
                                                                 Long displayProjetId) {
        return new IndustrialisationScoreResponse(
                candidature.getId(),
                displayProjetId,
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
                        scoreEliminatoire(result),
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

    private ComponentScore calculateCritereNoteScore(EvaluationProjectContext project,
                                                     Optional<Evaluation> latestEvaluation,
                                                     List<ScoreBreakdownDto> details,
                                                     List<BlocageEliminatoireDto> blocages,
                                                     List<String> elementsManquants) {
        if (latestEvaluation.isEmpty()) {
            Double snapshot = project.scoreSnapshot();
            if (snapshot != null) {
                details.add(new ScoreBreakdownDto(
                        CATEGORIE_CRITERES_NOTES,
                        "SNAPSHOT",
                        "Score evaluation projet",
                        round(snapshot),
                        CRITERES_NOTES_WEIGHT,
                        "SNAPSHOT_PROJET",
                        "Aucune evaluation detaillee trouvee; utilisation du score projet sauvegarde.",
                        true
                ));
                if (Boolean.TRUE.equals(project.bloqueParEliminatoire())) {
                    blocages.add(new BlocageEliminatoireDto(
                            "EVALUATION_PROJET",
                            project.eliminatoryReferenceId(),
                            "Critere eliminatoire projet",
                            "Le projet est marque bloque par un critere eliminatoire."
                    ));
                }
                return new ComponentScore(round(snapshot), true);
            }
            elementsManquants.add("Evaluation projet absente.");
            details.add(new ScoreBreakdownDto(
                    CATEGORIE_CRITERES_NOTES,
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
                    CATEGORIE_CRITERES_NOTES,
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
            double normalized = normalizedScore(resultat);
            weightedScores += normalized * weight;
            totalWeight += weight;
            details.add(new ScoreBreakdownDto(
                    CATEGORIE_CRITERES_NOTES,
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
                CATEGORIE_CRITERES_NOTES,
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

    /** Score d'un critere eliminatoire : 100 si OK, 0 sinon, {@code null} tant qu'il n'est pas evalue. */
    private Double scoreEliminatoire(ReponseEliminatoire result) {
        if (result == null) {
            return null;
        }
        return result == ReponseEliminatoire.OK ? 100.0 : 0.0;
    }

    /** Score du critere ramene sur 100, en privilegiant le score normalise fourni par le moteur ML. */
    private double normalizedScore(ResultatCritere resultat) {
        if (resultat.getNormalizedScore() != null) {
            return clamp(resultat.getNormalizedScore() * 100.0);
        }
        double note = resultat.getNoteValue() != null
                ? (double) resultat.getNoteValue()
                : resultat.getNoteObtenue();
        return clamp((note / noteScale(resultat)) * 100.0);
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

    record EvaluationProjectContext(
            Long displayProjetId,
            Optional<Evaluation> latestEvaluation,
            Double scoreSnapshot,
            Boolean bloqueParEliminatoire,
            Long eliminatoryReferenceId
    ) {
    }

    private record ComponentScore(Double score, boolean applicable) {
    }

    private record WeightedComponent(double weight, ComponentScore score) {
    }
}
