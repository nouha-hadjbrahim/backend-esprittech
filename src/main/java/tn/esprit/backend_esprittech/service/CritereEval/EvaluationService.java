package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationOverrideRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationValidationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlCriterionRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlCriterionResult;
import tn.esprit.backend_esprittech.dto.CritereEval.MlDeliverableContent;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvidenceResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.MlProjectContext;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatCritereResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatNoteRequest;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationService {

    private final SujetProjetRepository sujetProjetRepository;
    private final CritereEliminatoireRepository critereEliminatoireRepository;
    private final CritereNoteRepository critereNoteRepository;
    private final EvaluationRepository evaluationRepository;
    private final LivrableRepository livrableRepository;
    private final NoteLevelService noteLevelService;
    private final UserService userService;
    private final DeliverableContentExtractionService contentExtractionService;
    private final MlEvaluationClient mlEvaluationClient;

    @Value("${evaluation.ml.recalculation-cooldown-seconds:0}")
    private long recalculationCooldownSeconds;

    @Historise(action = ActionType.CREATE, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", newValuesExpression = "{'scoreFinal': #result.scoreFinal()}", summaryExpression = "'Évaluation manuelle projet (id=' + #projetId + ')'")
    public EvaluationResponse calculerEvaluation(Long projetId, EvaluationRequest request) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        EvaluationData data = buildManualEvaluation(request);
        return persistEvaluation(projet, data, resolveActorName(userService.getCurrentUser()), null);
    }

    @Historise(action = ActionType.RECALCULER_EVALUATION, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", newValuesExpression = "{'scoreFinal': #result.scoreFinal(), 'mlStatus': #result.mlStatus()}", summaryExpression = "'Évaluation automatique projet (id=' + #projetId + ')'")
    public EvaluationResponse calculateAutomaticEvaluation(Long projetId, String recalculationReason) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        User currentUser = userService.getCurrentUser();
        ensureCanCalculate(currentUser, projet);
        ensureNoDuplicateEvaluation(projetId);
        EvaluationData data = buildAutomaticEvaluationFromMl(projet);
        String effectiveReason = currentUser.getRole() == Role.ROLE_ENSEIGNANT
                ? "Nouveaux livrables deposes"
                : recalculationReason;
        return persistEvaluation(projet, data, resolveActorName(currentUser), effectiveReason);
    }

    public EvaluationResponse calculateEvaluationForSystem(Long projetId) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        EvaluationData data = buildAutomaticEvaluationFromMl(projet);
        return persistEvaluation(projet, data, "SYSTEM", "Calcul automatique apres terminaison du projet");
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getLatestEvaluationByProjet(Long projetId) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));
        ensureCanView(userService.getCurrentUser(), projet);

        Evaluation evaluation = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune evaluation trouvee pour le projet id: " + projetId));

        return toResponse(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluationHistoryByProjet(Long projetId) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));
        ensureCanView(userService.getCurrentUser(), projet);
        return evaluationRepository.findBySujetProjetIdOrderByDateCalculDesc(projetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Map<String, Object> getEvaluationOldValues(Long projetId) {
        Evaluation e = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(projetId).orElse(null);
        if (e == null) return Map.of();
        Map<String, Object> map = new HashMap<>();
        map.put("validationStatus", e.getValidationStatus());
        map.put("scoreFinal", e.getScoreFinal());
        map.put("finalValidatedScore", e.getFinalValidatedScore());
        return map;
    }

    @Historise(action = ActionType.ACCEPT, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", oldValuesExpression = "@evaluationService.getEvaluationOldValues(#projetId)", newValuesExpression = "{'validationStatus': 'VALIDATED', 'finalValidatedScore': #result.finalValidatedScore()}", summaryExpression = "'Validation évaluation projet (id=' + #projetId + ')'")
    public EvaluationResponse validateLatestEvaluation(Long projetId, EvaluationValidationRequest request) {
        SujetProjet projet = getProjet(projetId);
        User current = userService.getCurrentUser();
        ensureCanValidate(current);
        Evaluation evaluation = latestEvaluation(projetId);
        evaluation.setValidationStatus("VALIDATED");
        evaluation.setFinalValidatedScore(evaluation.getScoreFinal());
        evaluation.setValidatedBy(resolveActorName(current));
        evaluation.setValidatedAt(LocalDateTime.now());
        if (request != null && StringUtils.hasText(request.commentaire())) {
            evaluation.setCommentaire(request.commentaire().trim());
        }
        projet.setScoreFinalEvaluation(evaluation.getScoreFinal());
        sujetProjetRepository.save(projet);
        return toResponse(evaluationRepository.save(evaluation));
    }

    @Historise(action = ActionType.REFUSE, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", oldValuesExpression = "@evaluationService.getEvaluationOldValues(#projetId)", newValuesExpression = "{'validationStatus': 'REJECTED'}", summaryExpression = "'Rejet évaluation projet (id=' + #projetId + ')'")
    public EvaluationResponse rejectLatestEvaluation(Long projetId, EvaluationValidationRequest request) {
        User current = userService.getCurrentUser();
        ensureCanValidate(current);
        Evaluation evaluation = latestEvaluation(projetId);
        evaluation.setValidationStatus("REJECTED");
        evaluation.setValidatedBy(resolveActorName(current));
        evaluation.setValidatedAt(LocalDateTime.now());
        if (request != null && StringUtils.hasText(request.commentaire())) {
            evaluation.setCommentaire(request.commentaire().trim());
        }
        return toResponse(evaluationRepository.save(evaluation));
    }

    @Historise(action = ActionType.OVERRIDE, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", oldValuesExpression = "@evaluationService.getEvaluationOldValues(#projetId)", newValuesExpression = "{'validationStatus': 'OVERRIDDEN', 'finalValidatedScore': #result.finalValidatedScore()}", metadataExpression = "{'reason': #request.reason()}", summaryExpression = "'Surcharge évaluation projet (id=' + #projetId + ')'")
    public EvaluationResponse overrideLatestEvaluation(Long projetId, EvaluationOverrideRequest request) {
        SujetProjet projet = getProjet(projetId);
        User current = userService.getCurrentUser();
        ensureCanOverride(current);
        Evaluation evaluation = latestEvaluation(projetId);
        evaluation.setValidationStatus("OVERRIDDEN");
        evaluation.setFinalValidatedScore(round(request.finalScore()));
        evaluation.setOverrideReason(safeValue(request.reason()));
        evaluation.setValidatedBy(resolveActorName(current));
        evaluation.setValidatedAt(LocalDateTime.now());
        projet.setScoreFinalEvaluation(evaluation.getFinalValidatedScore());
        sujetProjetRepository.save(projet);
        return toResponse(evaluationRepository.save(evaluation));
    }

    private SujetProjet getProjetAndValidateTerminated(Long projetId) {
        SujetProjet projet = getProjet(projetId);

        if (projet.getStatut() != StatutSujet.REALISATION_TERMINEE) {
            throw new BadRequestException("Le projet doit etre en statut REALISATION_TERMINEE pour etre evalue.");
        }
        return projet;
    }

    private SujetProjet getProjet(Long projetId) {
        return sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));
    }

    private Evaluation latestEvaluation(Long projetId) {
        return evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune evaluation trouvee pour le projet id: " + projetId));
    }

    private EvaluationData buildManualEvaluation(EvaluationRequest request) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();
        Set<Integer> activeNoteValues = activeNoteValuesFor(criteresNotes);
        int maxActiveNoteValue = maxActiveNoteValue(activeNoteValues);

        Map<Long, ResultatEliminatoireRequest> resultatsEliminatoires = mapEliminatoires(request.eliminatoires());
        Map<Long, ResultatNoteRequest> resultatsNotes = mapNotes(request.notes());

        List<ResultatCritere> resultats = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        double sommeScoresCriteres = 0.0;
        double sommePoids = 0.0;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            ResultatEliminatoireRequest resultatRequest = resultatsEliminatoires.get(critere.getId());
            if (resultatRequest == null) {
                throw new BadRequestException("Resultat manquant pour le critere eliminatoire id: " + critere.getId());
            }

            ResultatCritere resultat = baseResult(critere, TypeCritere.ELIMINATOIRE);
            resultat.setModeEvaluation(ModeEvaluation.MANUAL_ADMIN_VALUE);
            resultat.setRuleConfigured(true);
            resultat.setReponseEliminatoire(resultatRequest.reponse());
            resultat.setEvidenceSummary("Valeur saisie manuellement.");
            resultat.setExplanation("Critere eliminatoire evalue manuellement.");
            resultat.setCommentaire(resultatRequest.commentaire());
            resultats.add(resultat);

            if (resultatRequest.reponse() == ReponseEliminatoire.NOT_OK) {
                warnings.add(critere.getLibelle());
            }
        }

        for (CritereNote critere : criteresNotes) {
            ResultatNoteRequest resultatRequest = resultatsNotes.get(critere.getId());
            if (resultatRequest == null) {
                throw new BadRequestException("Resultat manquant pour le critere note id: " + critere.getId());
            }

            int noteValue = resolveManualNoteValue(resultatRequest);
            validateNoteValue(noteValue, critere, activeNoteValues);
            double poids = positiveWeight(critere.getPoids());
            double scoreCritere = scoreCritere(noteValue, poids, maxActiveNoteValue);
            sommeScoresCriteres += scoreCritere;
            sommePoids += poids;

            ResultatCritere resultat = baseResult(critere, TypeCritere.NOTE);
            resultat.setModeEvaluation(ModeEvaluation.MANUAL_ADMIN_VALUE);
            resultat.setRuleConfigured(true);
            applyNoteResult(resultat, noteValue, poids, scoreCritere, maxActiveNoteValue,
                    "MANUAL_ADMIN_VALUE", "Valeur saisie manuellement.", "Note saisie manuellement.");
            resultat.setCommentaire(resultatRequest.commentaire());
            resultats.add(resultat);
        }

        return buildEvaluationData(resultats, warnings, sommeScoresCriteres, sommePoids);
    }

    private EvaluationData buildAutomaticEvaluationFromMl(SujetProjet projet) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();
        Set<Integer> activeNoteValues = activeNoteValuesFor(criteresNotes);
        int maxActiveNoteValue = maxActiveNoteValue(activeNoteValues);
        List<Livrable> livrables = livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(projet.getId());
        List<MlDeliverableContent> extractedContent = contentExtractionService.extract(livrables);
        MlEvaluationResponse mlResponse = mlEvaluationClient.evaluate(
                buildMlRequest(projet, criteresEliminatoires, criteresNotes, extractedContent, maxActiveNoteValue)
        );
        validateMlResponse(mlResponse, criteresEliminatoires, criteresNotes);
        Map<Long, MlCriterionResult> mlResults = mlResponse.results()
                .stream()
                .collect(Collectors.toMap(MlCriterionResult::criterionId, Function.identity()));

        List<ResultatCritere> resultats = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        double sommeScoresCriteres = 0.0;
        double sommePoids = 0.0;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            ResultatCritere resultat = baseResult(critere, TypeCritere.ELIMINATOIRE);
            MlCriterionResult mlResult = mlResults.get(critere.getId());
            boolean satisfied = Boolean.TRUE.equals(mlResult.satisfied());
            ReponseEliminatoire resultatAuto = satisfied ? ReponseEliminatoire.OK : ReponseEliminatoire.NOT_OK;
            resultat.setRuleConfigured(true);
            resultat.setModeEvaluation(ModeEvaluation.CONTENT_ANALYSIS);
            resultat.setReponseEliminatoire(resultatAuto);
            applyMlResult(resultat, mlResult, evidenceSummary(mlResult), evidenceLivrableIds(mlResult));
            resultat.setCommentaire(resultat.getExplanation());
            resultats.add(resultat);

            if (resultatAuto == ReponseEliminatoire.NOT_OK) {
                warnings.add(critere.getLibelle());
            }
        }

        for (CritereNote critere : criteresNotes) {
            MlCriterionResult mlResult = mlResults.get(critere.getId());
            double normalized = normalizedScore(mlResult);
            int noteValue = noteValueFromNormalized(normalized, maxActiveNoteValue);
            double poids = positiveWeight(critere.getPoids());
            double score = normalized * poids;
            sommeScoresCriteres += score;
            sommePoids += poids;

            ResultatCritere resultat = baseResult(critere, TypeCritere.NOTE);
            resultat.setRuleConfigured(true);
            resultat.setModeEvaluation(ModeEvaluation.CONTENT_ANALYSIS);
            applyNoteResult(resultat, noteValue, poids, score, maxActiveNoteValue,
                    "ML_CONTENT_ANALYSIS", null, mlResult.justification());
            applyMlResult(resultat, mlResult, evidenceSummary(mlResult), evidenceLivrableIds(mlResult));
            resultat.setCommentaire(mlResult.justification());
            resultats.add(resultat);
        }

        return buildEvaluationData(
                resultats,
                warnings,
                sommeScoresCriteres,
                sommePoids,
                mlResponse
        );
    }

    private EvaluationData buildEvaluationData(List<ResultatCritere> resultats,
                                               List<String> eliminatoryWarnings,
                                               double sommeScoresCriteres,
                                               double sommePoids) {
        return buildEvaluationData(resultats, eliminatoryWarnings, sommeScoresCriteres, sommePoids, null);
    }

    private EvaluationData buildEvaluationData(List<ResultatCritere> resultats,
                                               List<String> eliminatoryWarnings,
                                               double sommeScoresCriteres,
                                               double sommePoids,
                                               MlEvaluationResponse mlResponse) {
        double scoreFinal = sommePoids > 0 ? round((sommeScoresCriteres / sommePoids) * 100) : 0.0;
        boolean hasWarnings = !eliminatoryWarnings.isEmpty();
        String commentaire = hasWarnings
                ? "Projet evalue avec alertes eliminatoires non bloquantes."
                : "Projet evalue avec succes.";
        return new EvaluationData(
                resultats,
                scoreFinal,
                true,
                hasWarnings,
                eliminatoryWarnings,
                commentaire,
                mlResponse == null ? "MANUAL" : safeValue(mlResponse.status()),
                mlResponse == null ? null : mlResponse.modelVersion(),
                mlResponse == null ? null : mlResponse.globalConfidence(),
                mlResponse == null ? scoreFinal : scoreFinal,
                mlResponse == null ? List.of() : safeList(mlResponse.warnings()),
                mlResponse == null ? List.of() : safeList(mlResponse.strengths()),
                mlResponse == null ? List.of() : safeList(mlResponse.weaknesses()),
                mlResponse == null ? List.of() : safeList(mlResponse.recommendations()),
                null
        );
    }

    private EvaluationResponse persistEvaluation(SujetProjet projet,
                                                 EvaluationData data,
                                                 String calculatedBy,
                                                 String recalculationReason) {
        Evaluation evaluation = new Evaluation();
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(data.scoreFinal());
        evaluation.setMlScore(data.mlScore());
        evaluation.setMlStatus(data.mlStatus());
        evaluation.setMlModelVersion(data.mlModelVersion());
        evaluation.setMlGlobalConfidence(data.mlGlobalConfidence());
        evaluation.setMlWarnings(joinLines(data.mlWarnings()));
        evaluation.setStrengths(joinLines(data.strengths()));
        evaluation.setWeaknesses(joinLines(data.weaknesses()));
        evaluation.setRecommendations(joinLines(data.recommendations()));
        evaluation.setFinalValidatedScore(null);
        evaluation.setValidationStatus("PENDING");
        evaluation.setErrorMessage(data.errorMessage());
        evaluation.setEligibleIndustrialisation(data.eligibleIndustrialisation());
        evaluation.setBloqueParEliminatoire(data.hasEliminatoryWarnings());
        evaluation.setHasEliminatoryWarnings(data.hasEliminatoryWarnings());
        evaluation.setEliminatoryWarningsCount(data.eliminatoryWarnings().size());
        evaluation.setEliminatoryWarningsDetails(String.join("\n", data.eliminatoryWarnings()));
        evaluation.setCommentaire(data.commentaire());
        evaluation.setCalculatedBy(calculatedBy);
        evaluation.setRecalculationReason(recalculationReason);
        data.resultats().forEach(evaluation::addResultat);

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        projet.setScoreFinalEvaluation(data.scoreFinal());
        projet.setEligibleIndustrialisation(true);
        projet.setBloqueParEliminatoire(data.hasEliminatoryWarnings());
        projet.setHasEliminatoryWarnings(data.hasEliminatoryWarnings());
        projet.setEliminatoryWarningsCount(data.eliminatoryWarnings().size());
        sujetProjetRepository.save(projet);

        return toResponse(savedEvaluation);
    }

    private void applyNoteResult(ResultatCritere resultat,
                                 int noteValue,
                                 double poids,
                                 double scoreCritere,
                                 int maxActiveNoteValue,
                                 String matchedRule,
                                 String metadataUsed,
                                 String explanation) {
        resultat.setNoteValue(noteValue);
        resultat.setNoteLabel(noteValue == 0 ? "Non evaluable" : noteLevelService.labelFor(noteValue));
        resultat.setNoteObtenue((double) noteValue);
        resultat.setBareme((double) maxActiveNoteValue);
        resultat.setPoids(poids);
        resultat.setScorePondere(round(scoreCritere));
        resultat.setMatchedRule(matchedRule);
        resultat.setMetadataUsed(metadataUsed);
        resultat.setExplanation(explanation);
        resultat.setEvidenceSummary(metadataUsed);
    }

    private MlEvaluationRequest buildMlRequest(SujetProjet projet,
                                               List<CritereEliminatoire> criteresEliminatoires,
                                               List<CritereNote> criteresNotes,
                                               List<MlDeliverableContent> extractedContent,
                                               int maxActiveNoteValue) {
        List<MlCriterionRequest> criteria = new ArrayList<>();
        criteresEliminatoires.forEach(critere -> criteria.add(new MlCriterionRequest(
                critere.getId(),
                TypeCritere.ELIMINATOIRE.name(),
                critere.getLibelle(),
                critere.getDescription(),
                critere.getDomaine(),
                1.0,
                0.0,
                true
        )));
        criteresNotes.forEach(critere -> criteria.add(new MlCriterionRequest(
                critere.getId(),
                TypeCritere.NOTE.name(),
                critere.getLibelle(),
                critere.getDescription(),
                critere.getDomaine(),
                (double) maxActiveNoteValue,
                positiveWeight(critere.getPoids()),
                false
        )));
        return new MlEvaluationRequest(
                new MlProjectContext(
                        projet.getId(),
                        safeValue(projet.getTitre()),
                        safeValue(projet.getDomaine()),
                        safeValue(projet.getTechnologies()),
                        projet.getCategorie() == null ? null : projet.getCategorie().name()
                ),
                criteria,
                extractedContent
        );
    }

    private void validateMlResponse(MlEvaluationResponse response,
                                    List<CritereEliminatoire> criteresEliminatoires,
                                    List<CritereNote> criteresNotes) {
        Set<Long> expectedIds = new java.util.HashSet<>();
        criteresEliminatoires.forEach(critere -> expectedIds.add(critere.getId()));
        criteresNotes.forEach(critere -> expectedIds.add(critere.getId()));
        Set<Long> returnedIds = response.results().stream()
                .map(MlCriterionResult::criterionId)
                .collect(Collectors.toSet());
        if (!returnedIds.containsAll(expectedIds)) {
            throw new BusinessException("La reponse ML ne couvre pas tous les criteres actifs.");
        }
        response.results().forEach(result -> {
            if (result.normalizedScore() == null && (result.score() == null || result.maxScore() == null)) {
                throw new BusinessException("La reponse ML contient un score invalide pour le critere " + result.criterionId());
            }
            if (result.confidence() == null || result.confidence() < 0.0 || result.confidence() > 1.0) {
                throw new BusinessException("La confiance ML est invalide pour le critere " + result.criterionId());
            }
        });
    }

    private void applyMlResult(ResultatCritere resultat,
                               MlCriterionResult mlResult,
                               String evidenceSummary,
                               String evidenceLivrableIds) {
        resultat.setMlScore(nullToZero(mlResult.score()));
        resultat.setMlMaxScore(nullToZero(mlResult.maxScore()));
        resultat.setNormalizedScore(normalizedScore(mlResult));
        resultat.setConfidence(nullToZero(mlResult.confidence()));
        resultat.setExplanation(mlResult.justification());
        resultat.setEvidenceSummary(evidenceSummary);
        resultat.setEvidenceLivrableIds(evidenceLivrableIds);
        resultat.setStrengths(joinLines(safeList(mlResult.strengths())));
        resultat.setWeaknesses(joinLines(safeList(mlResult.weaknesses())));
        resultat.setRecommendations(joinLines(safeList(mlResult.recommendations())));
        resultat.setMetadataUsed(null);
    }

    private double normalizedScore(MlCriterionResult mlResult) {
        if (mlResult.normalizedScore() != null) {
            return clamp01(mlResult.normalizedScore());
        }
        double maxScore = nullToZero(mlResult.maxScore());
        if (maxScore <= 0.0) {
            return 0.0;
        }
        return clamp01(nullToZero(mlResult.score()) / maxScore);
    }

    private int noteValueFromNormalized(double normalized, int maxActiveNoteValue) {
        if (normalized <= 0.0 || maxActiveNoteValue <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(maxActiveNoteValue, (int) Math.round(normalized * maxActiveNoteValue)));
    }

    private String evidenceSummary(MlCriterionResult mlResult) {
        List<MlEvidenceResponse> evidence = safeList(mlResult.evidence());
        if (evidence.isEmpty()) {
            return mlResult.justification();
        }
        return evidence.stream()
                .limit(3)
                .map(MlEvidenceResponse::excerpt)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" | "));
    }

    private String evidenceLivrableIds(MlCriterionResult mlResult) {
        return safeList(mlResult.evidence()).stream()
                .map(MlEvidenceResponse::deliverableId)
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private ResultatCritere baseResult(Long critereId, String libelle, String domaine, TypeCritere typeCritere) {
        ResultatCritere resultat = new ResultatCritere();
        resultat.setCritereId(critereId);
        resultat.setCritereLibelle(libelle);
        resultat.setCritereDomaine(domaine);
        resultat.setTypeCritere(typeCritere);
        return resultat;
    }

    private ResultatCritere baseResult(CritereEvaluation critere, TypeCritere typeCritere) {
        ResultatCritere resultat = baseResult(critere.getId(), critere.getLibelle(), critere.getDomaine(), typeCritere);
        resultat.setCritereDescription(critere.getDescription());
        resultat.setModeEvaluation(ModeEvaluation.CONTENT_ANALYSIS);
        resultat.setRuleConfigured(true);
        return resultat;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private Set<Integer> activeNoteValuesFor(List<CritereNote> criteresNotes) {
        return criteresNotes.isEmpty() ? Set.of() : noteLevelService.activeValues();
    }

    private int maxActiveNoteValue(Set<Integer> activeNoteValues) {
        return activeNoteValues.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private int resolveManualNoteValue(ResultatNoteRequest request) {
        if (request.noteValue() != null) {
            return request.noteValue();
        }
        if (request.noteObtenue() == null) {
            throw new BadRequestException("La note est obligatoire pour le critere id: " + request.critereId());
        }
        return request.noteObtenue().intValue();
    }

    private void validateNoteValue(Integer noteValue, CritereNote critere, Set<Integer> activeNoteValues) {
        if (noteValue == null) {
            throw new BadRequestException("La note est obligatoire pour le critere id: " + critere.getId());
        }
        if (noteValue < 1) {
            throw new BadRequestException("La note doit etre superieure ou egale a 1 pour le critere id: " + critere.getId());
        }
        if (!activeNoteValues.contains(noteValue)) {
            throw new BadRequestException("La note doit correspondre a un niveau actif configure pour le critere id: " + critere.getId());
        }
        if (critere.getPoids() == null || critere.getPoids() <= 0) {
            throw new BadRequestException("Le poids du critere doit etre superieur a 0.");
        }
    }

    private double positiveWeight(Double value) {
        return value == null || value <= 0.0 ? 1.0 : value;
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double clamp01(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String joinLines(List<String> values) {
        return safeList(values).stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    private List<String> splitLines(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private double scoreCritere(int noteValue, double poids, int maxActiveNoteValue) {
        if (maxActiveNoteValue <= 0) {
            throw new BusinessConflictException(NoteLevelService.NO_ACTIVE_NOTE_LEVEL_MESSAGE);
        }
        return (noteValue / (double) maxActiveNoteValue) * poids;
    }

    private void ensureNoDuplicateEvaluation(Long projetId) {
        if (recalculationCooldownSeconds <= 0) {
            return;
        }
        Evaluation latest = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(projetId).orElse(null);
        if (latest == null || latest.getDateCalcul() == null) {
            return;
        }
        LocalDateTime allowedAt = latest.getDateCalcul().plusSeconds(recalculationCooldownSeconds);
        if (allowedAt.isAfter(LocalDateTime.now())) {
            long retryAfterSeconds = Math.max(1, java.time.Duration.between(LocalDateTime.now(), allowedAt).toSeconds());
            throw new BusinessConflictException(
                    "Une evaluation vient deja d'etre calculee. Reessayez dans " + retryAfterSeconds + " seconde(s)."
            );
        }
    }

    private void ensureCanCalculate(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (current.getRole() == Role.ROLE_ENSEIGNANT && projet.getEncadrantId().equals(current.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous n'etes pas autorise a recalculer le score de ce projet.");
    }

    private void ensureCanView(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        if (current.getRole() == Role.ROLE_ENSEIGNANT && projet.getEncadrantId().equals(current.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous n'etes pas autorise a consulter cette evaluation.");
    }

    private void ensureCanValidate(User current) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        throw new AccessDeniedException("Vous n'etes pas autorise a valider cette evaluation.");
    }

    private void ensureCanOverride(User current) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        throw new AccessDeniedException("Seul un administrateur peut surcharger le score final.");
    }

    private String resolveActorName(User currentUser) {
        return currentUser.getRole().name() + " " + currentUser.getPrenom() + " " + currentUser.getNom();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<Long, ResultatEliminatoireRequest> mapEliminatoires(List<ResultatEliminatoireRequest> requests) {
        Map<Long, ResultatEliminatoireRequest> map = new HashMap<>();
        if (requests == null) {
            return map;
        }
        for (ResultatEliminatoireRequest request : requests) {
            map.put(request.critereId(), request);
        }
        return map;
    }

    private Map<Long, ResultatNoteRequest> mapNotes(List<ResultatNoteRequest> requests) {
        Map<Long, ResultatNoteRequest> map = new HashMap<>();
        if (requests == null) {
            return map;
        }
        for (ResultatNoteRequest request : requests) {
            map.put(request.critereId(), request);
        }
        return map;
    }

    private List<Long> parseEvidenceLivrableIds(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> splitWarningDetails(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private EvaluationResponse toResponse(Evaluation evaluation) {
        List<ResultatCritereResponse> resultats = evaluation.getResultats()
                .stream()
                .map(resultat -> new ResultatCritereResponse(
                        resultat.getId(),
                        resultat.getCritereId(),
                        resultat.getCritereLibelle(),
                        resultat.getCritereDescription(),
                        resultat.getCritereDomaine(),
                        resultat.getTypeCritere(),
                        resultat.getModeEvaluation(),
                        resultat.getReponseEliminatoire(),
                        resultat.getNoteObtenue(),
                        resultat.getScorePondere(),
                        resultat.getBareme(),
                        resultat.getPoids(),
                        resultat.getNoteValue(),
                        resultat.getNoteLabel(),
                        resultat.getMatchedRule(),
                        resultat.getMetadataUsed(),
                        resultat.getExplanation(),
                        resultat.getEvidenceSummary(),
                        parseEvidenceLivrableIds(resultat.getEvidenceLivrableIds()),
                        resultat.getRuleConfigured(),
                        resultat.getCommentaire(),
                        resultat.getMlScore(),
                        resultat.getMlMaxScore(),
                        resultat.getNormalizedScore(),
                        resultat.getConfidence(),
                        splitLines(resultat.getStrengths()),
                        splitLines(resultat.getWeaknesses()),
                        splitLines(resultat.getRecommendations())
                ))
                .toList();
        int totalNotes = (int) resultats.stream().filter(r -> r.typeCritere() == TypeCritere.NOTE).count();
        int notesConfigures = (int) resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.NOTE)
                .filter(r -> Boolean.TRUE.equals(r.ruleConfigured()))
                .count();
        int totalEliminatoires = (int) resultats.stream().filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE).count();
        int eliminatoiresSatisfaits = (int) resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> r.reponseEliminatoire() == ReponseEliminatoire.OK)
                .count();
        int eliminatoiresNonConfigures = (int) resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> !Boolean.TRUE.equals(r.ruleConfigured()))
                .count();
        int notesNonConfigures = totalNotes - notesConfigures;
        boolean evaluationComplete = resultats.stream().allMatch(r -> Boolean.TRUE.equals(r.ruleConfigured()));
        List<String> warningCriteriaNames = resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> r.reponseEliminatoire() == ReponseEliminatoire.NOT_OK)
                .map(ResultatCritereResponse::critereLibelle)
                .toList();
        String coverageMessage = "Score calcule sur " + totalNotes
                + " critere(s) note(s) actif(s) a partir du contenu extrait des livrables.";
        if (notesNonConfigures > 0 || eliminatoiresNonConfigures > 0) {
            coverageMessage += " " + (notesNonConfigures + eliminatoiresNonConfigures)
                    + " critere(s) utilisent une configuration incomplete.";
        }

        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getSujetProjet().getId(),
                evaluation.getScoreFinal(),
                evaluation.getEligibleIndustrialisation(),
                evaluation.getBloqueParEliminatoire(),
                evaluation.getHasEliminatoryWarnings(),
                evaluation.getEliminatoryWarningsCount(),
                splitWarningDetails(evaluation.getEliminatoryWarningsDetails()),
                evaluation.getDateCalcul(),
                evaluation.getCommentaire(),
                evaluation.getCalculatedBy(),
                evaluation.getRecalculationReason(),
                resultats,
                totalNotes,
                notesConfigures,
                notesNonConfigures,
                totalEliminatoires,
                eliminatoiresSatisfaits,
                eliminatoiresNonConfigures,
                evaluationComplete,
                coverageMessage,
                warningCriteriaNames,
                evaluation.getMlStatus(),
                evaluation.getMlModelVersion(),
                evaluation.getMlGlobalConfidence(),
                evaluation.getMlScore(),
                evaluation.getFinalValidatedScore(),
                evaluation.getValidationStatus(),
                evaluation.getValidatedBy(),
                evaluation.getValidatedAt(),
                evaluation.getOverrideReason(),
                splitLines(evaluation.getMlWarnings()),
                splitLines(evaluation.getStrengths()),
                splitLines(evaluation.getWeaknesses()),
                splitLines(evaluation.getRecommendations()),
                evaluation.getErrorMessage()
        );
    }

    private record EvaluationData(
            List<ResultatCritere> resultats,
            Double scoreFinal,
            Boolean eligibleIndustrialisation,
            Boolean hasEliminatoryWarnings,
            List<String> eliminatoryWarnings,
            String commentaire,
            String mlStatus,
            String mlModelVersion,
            Double mlGlobalConfidence,
            Double mlScore,
            List<String> mlWarnings,
            List<String> strengths,
            List<String> weaknesses,
            List<String> recommendations,
            String errorMessage
    ) {
    }
}
