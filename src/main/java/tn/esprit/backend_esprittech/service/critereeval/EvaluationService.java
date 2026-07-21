package tn.esprit.backend_esprittech.service.critereeval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationOverrideRequest;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.critereeval.EvaluationValidationRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CriterionConfigurationIssueResponse;
import tn.esprit.backend_esprittech.dto.critereeval.MlCriterionRequest;
import tn.esprit.backend_esprittech.dto.critereeval.MlCriterionResult;
import tn.esprit.backend_esprittech.dto.critereeval.MlDeliverableContent;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvaluationRequest;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvaluationResponse;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvidenceResponse;
import tn.esprit.backend_esprittech.dto.critereeval.MlExternalSourceRequest;
import tn.esprit.backend_esprittech.dto.critereeval.MlProjectContext;
import tn.esprit.backend_esprittech.dto.critereeval.ResultatCritereResponse;
import tn.esprit.backend_esprittech.dto.critereeval.ResultatEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.critereeval.ResultatNoteRequest;
import tn.esprit.backend_esprittech.entity.critereeval.CritereEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.CritereEvaluation;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNote;
import tn.esprit.backend_esprittech.entity.critereeval.Evaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.exception.BusinessConflictException;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.EvaluationPersistenceException;
import tn.esprit.backend_esprittech.exception.critereeval.BadRequestException;
import tn.esprit.backend_esprittech.exception.critereeval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.critereeval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.critereeval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.critereeval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;
import tn.esprit.backend_esprittech.service.projetcatalogue.ProjetCatalogueService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationService {
    private static final String VERSION_MOTEUR_ML = "multimodal-semantic-v2";
    private static final String MSG_PROJET_INTROUVABLE = "Projet introuvable avec id: ";
    private static final String MSG_AUCUNE_EVALUATION = "Aucune evaluation trouvee pour le projet id: ";
    private static final String SUFFIXE_SUR_CENT = "/100.";
    private static final String PREFIXE_EVALUATION_PROJET = "L'evaluation de votre projet \"";
    private static final String LIEN_MES_PROJETS = "/frontoffice/mes-projets";
    private static final String STATUT_NOT_OK = "NOT_OK";
    private static final String STATUT_INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
    private static final String STATUT_INDETERMINATE = "INDETERMINATE";
    private static final String STATUT_FAILED_RETRYABLE = "FAILED_RETRYABLE";
    private static final String STATUT_CONFIGURATION_INCOMPLETE = "CONFIGURATION_INCOMPLETE";


    private final SujetProjetRepository sujetProjetRepository;
    private final CritereEliminatoireRepository critereEliminatoireRepository;
    private final CritereNoteRepository critereNoteRepository;
    private final EvaluationRepository evaluationRepository;
    private final LivrableRepository livrableRepository;
    private final NoteLevelService noteLevelService;
    private final UserService userService;
    private final DeliverableContentExtractionService contentExtractionService;
    private final MlEvaluationClient mlEvaluationClient;
    private final ObjectMapper objectMapper;
    private final ProjetCatalogueService projetCatalogueService;
    private final NotificationService notificationService;
    private final ProjetCatalogueRepository projetCatalogueRepository;
    private final LivrableCatalogueRepository livrableCatalogueRepository;

    @Value("${evaluation.ml.recalculation-cooldown-seconds:0}")
    private long recalculationCooldownSeconds;

    @Value("${evaluation.engine:multimodal-semantic-v2}")
    private String evaluationEngine = VERSION_MOTEUR_ML;

    @Value("${evaluation.criteria-version:2026-07}")
    private String criteriaVersion = "2026-07";

    public EvaluationResponse calculerEvaluation(Long projetId, EvaluationRequest request) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        EvaluationData data = buildManualEvaluation(request);
        return persistEvaluation(projet, data, resolveActorName(userService.getCurrentUser()), null);
    }

    @Historise(action = ActionType.RECALCULER_EVALUATION, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", newValuesExpression = "{'scoreFinal': #result.scoreFinal(), 'mlStatus': #result.mlStatus()}", summaryExpression = "'Évaluation automatique projet ' + @evaluationService.getProjetTitreById(#projetId)")
    public EvaluationResponse calculateAutomaticEvaluation(Long projetId, String recalculationReason) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        User currentUser = userService.getCurrentUser();
        ensureCanCalculate(currentUser, projet);
        ensureNoDuplicateEvaluation(projetId);
        EvaluationTrigger trigger = currentUser.getRole() == Role.ROLE_ADMIN
                ? EvaluationTrigger.ADMIN_RECALCULATION
                : EvaluationTrigger.MANUAL_RECALCULATION;
        String correlationId = UUID.randomUUID().toString();
        EvaluationData data = buildAutomaticEvaluationFromMl(projet, trigger, correlationId);
        String effectiveReason = currentUser.getRole() == Role.ROLE_ENSEIGNANT
                ? "Nouveaux livrables deposes"
                : recalculationReason;
        return persistEvaluation(projet, data, resolveActorName(currentUser), effectiveReason);
    }

    public EvaluationResponse calculateEvaluationForSystem(Long projetId) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        EvaluationData data = buildAutomaticEvaluationFromMl(
                projet,
                EvaluationTrigger.AUTOMATIC_TERMINATION,
                UUID.randomUUID().toString()
        );
        return persistEvaluation(projet, data, "SYSTEM", "Calcul automatique apres terminaison du projet");
    }

    public EvaluationRunResult evaluateProject(Long projectId,
                                               EvaluationTrigger trigger,
                                               String correlationId) {
        SujetProjet projet = getProjetAndValidateTerminated(projectId);
        User currentUser = userService.getCurrentUser();
        ensureCanCalculate(currentUser, projet);
        ensureNoDuplicateEvaluation(projectId);
        String effectiveCorrelationId = StringUtils.hasText(correlationId)
                ? correlationId
                : UUID.randomUUID().toString();
        EvaluationData data = buildAutomaticEvaluationFromMl(projet, trigger, effectiveCorrelationId);
        EvaluationResponse response = persistEvaluation(
                projet,
                data,
                resolveActorName(currentUser),
                trigger.name()
        );
        return new EvaluationRunResult(
                projectId,
                trigger,
                effectiveCorrelationId,
                data.inputSnapshotHash(),
                data.pipelineVersion(),
                response
        );
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getLatestEvaluationByProjet(Long projetId) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
        ensureCanView(userService.getCurrentUser(), projet);

        Evaluation evaluation = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_AUCUNE_EVALUATION + projetId));

        return toResponse(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluationHistoryByProjet(Long projetId) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
        ensureCanView(userService.getCurrentUser(), projet);
        return evaluationRepository.findBySujetProjetIdOrderByDateCalculDescIdDesc(projetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public String getProjetTitreById(Long projetId) {
        return sujetProjetRepository.findById(projetId)
                .map(SujetProjet::getTitre)
                .orElse("Inconnu");
    }

    public String getProjetCatalogueTitreById(Long projetId) {
        return projetCatalogueRepository.findById(projetId)
                .map(ProjetCatalogue::getTitre)
                .orElse("Inconnu");
    }

    // ── Évaluation directe d'un projet du catalogue applicatif ──────────────
    // Le moteur ML est le même que pour les sujets : seule la source des données projet
    // ou livrables change. Permet d'évaluer un projet catalogue déposé manuellement,
    // sans sujet d'origine.

    @Historise(action = ActionType.RECALCULER_EVALUATION, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", newValuesExpression = "{'scoreFinal': #result.scoreFinal(), 'mlStatus': #result.mlStatus()}", summaryExpression = "'Évaluation automatique projet catalogue ' + @evaluationService.getProjetCatalogueTitreById(#projetId)")
    public EvaluationResponse calculateAutomaticEvaluationForCatalogue(Long projetId, String recalculationReason) {
        ProjetCatalogue projet = getProjetCatalogueAndValidateValide(projetId);
        User currentUser = userService.getCurrentUser();
        ensureCanCalculateCatalogue(currentUser, projet);
        ensureNoDuplicateEvaluationCatalogue(projetId);
        EvaluationTrigger trigger = currentUser.getRole() == Role.ROLE_ADMIN
                ? EvaluationTrigger.ADMIN_RECALCULATION
                : EvaluationTrigger.MANUAL_RECALCULATION;
        String correlationId = UUID.randomUUID().toString();
        EvaluationData data = buildAutomaticEvaluationFromMlCatalogue(projet, trigger, correlationId);
        String effectiveReason = currentUser.getRole() == Role.ROLE_ENSEIGNANT
                ? "Nouveaux livrables deposes"
                : recalculationReason;
        return persistEvaluationCatalogue(projet, data, resolveActorName(currentUser), effectiveReason);
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getLatestEvaluationByProjetCatalogue(Long projetId) {
        ProjetCatalogue projet = projetCatalogueRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
        ensureCanViewCatalogue(userService.getCurrentUser(), projet);
        return resolveLatestCatalogueOrSujetEvaluation(projet);
    }

    /**
     * Dernière évaluation d'un projet publié au catalogue (tout utilisateur authentifié) :
     * évaluation catalogue si présente, sinon évaluation ML du sujet d'origine.
     */
    @Transactional(readOnly = true)
    public EvaluationResponse getLatestEvaluationForPublishedCatalogue(Long projetId) {
        ProjetCatalogue projet = projetCatalogueRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
        if (!StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())) {
            throw new ResourceNotFoundException("Projet introuvable dans le catalogue (id=" + projetId + ")");
        }
        return resolveLatestCatalogueOrSujetEvaluation(projet);
    }

    private EvaluationResponse resolveLatestCatalogueOrSujetEvaluation(ProjetCatalogue projet) {
        return evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(projet.getId())
                .map(this::toResponse)
                .or(() -> {
                    Long sujetId = projet.getSujetId();
                    if (sujetId == null) {
                        return java.util.Optional.empty();
                    }
                    return evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(sujetId)
                            .map(this::toResponse);
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        MSG_AUCUNE_EVALUATION + projet.getId()));
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluationHistoryByProjetCatalogue(Long projetId) {
        ProjetCatalogue projet = projetCatalogueRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
        ensureCanViewCatalogue(userService.getCurrentUser(), projet);
        return evaluationRepository.findByProjetCatalogueIdOrderByDateCalculDescIdDesc(projetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getEvaluationDetailByProjetCatalogue(Long projetId, Long evaluationId) {
        ProjetCatalogue projet = projetCatalogueRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
        ensureCanViewCatalogue(userService.getCurrentUser(), projet);
        Evaluation evaluation = evaluationRepository.findByIdAndProjetCatalogueId(evaluationId, projetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation introuvable avec id: " + evaluationId + " pour le projet catalogue id: " + projetId
                ));
        return toResponse(evaluation);
    }

    public Map<String, Object> getOldValues(Long projetId) {
        Evaluation e = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(projetId).orElse(null);
        if (e == null) return Map.of();
        Map<String, Object> map = new HashMap<>();
        map.put("validationStatus", e.getValidationStatus());
        map.put("scoreFinal", e.getScoreFinal());
        map.put("finalValidatedScore", e.getFinalValidatedScore());
        return map;
    }

    @Historise(action = ActionType.ACCEPT, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", oldValuesExpression = "@evaluationService.getOldValues(#projetId)", newValuesExpression = "{'validationStatus': 'VALIDATED', 'finalValidatedScore': #result.finalValidatedScore()}", summaryExpression = "'Validation évaluation projet ' + @evaluationService.getProjetTitreById(#projetId)")
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
        EvaluationResponse response = toResponse(evaluationRepository.save(evaluation));

        notificationService.creerNotification(
                projet.getEncadrantId(),
                NotificationType.EVALUATION_TERMINEE,
                "Evaluation terminee",
                PREFIXE_EVALUATION_PROJET + projet.getTitre() + "\" a ete validee avec un score de " + evaluation.getScoreFinal() + SUFFIXE_SUR_CENT,
                LIEN_MES_PROJETS);

        return response;
    }

    @Historise(action = ActionType.REFUSE, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", oldValuesExpression = "@evaluationService.getOldValues(#projetId)", newValuesExpression = "{'validationStatus': 'REJECTED'}", summaryExpression = "'Rejet évaluation projet ' + @evaluationService.getProjetTitreById(#projetId)")
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
        EvaluationResponse response = toResponse(evaluationRepository.save(evaluation));

        SujetProjet projet = getProjet(projetId);
        notificationService.creerNotification(
                projet.getEncadrantId(),
                NotificationType.EVALUATION_TERMINEE,
                "Evaluation refusee",
                PREFIXE_EVALUATION_PROJET + projet.getTitre() + "\" a ete refusee.",
                LIEN_MES_PROJETS);

        return response;
    }

    @Historise(action = ActionType.OVERRIDE, entityType = EntityType.EVALUATION, entityIdExpression = "#result.id()", oldValuesExpression = "@evaluationService.getOldValues(#projetId)", newValuesExpression = "{'validationStatus': 'OVERRIDDEN', 'finalValidatedScore': #result.finalValidatedScore()}", metadataExpression = "{'reason': #request.reason()}", summaryExpression = "'Surcharge évaluation projet ' + @evaluationService.getProjetTitreById(#projetId)")
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
        EvaluationResponse response = toResponse(evaluationRepository.save(evaluation));

        notificationService.creerNotification(
                projet.getEncadrantId(),
                NotificationType.EVALUATION_TERMINEE,
                "Evaluation surchargee",
                PREFIXE_EVALUATION_PROJET + projet.getTitre() + "\" a ete surchargee avec le score " + evaluation.getFinalValidatedScore() + SUFFIXE_SUR_CENT,
                LIEN_MES_PROJETS);

        return response;
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
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
    }

    private Evaluation latestEvaluation(Long projetId) {
        return evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_AUCUNE_EVALUATION + projetId));
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
                    new TraceCalcul("MANUAL_ADMIN_VALUE", "Valeur saisie manuellement.",
                            "Note saisie manuellement."));
            resultat.setCommentaire(resultatRequest.commentaire());
            resultats.add(resultat);
        }

        return buildEvaluationData(resultats, warnings, new ScoreAgregat(sommeScoresCriteres, sommePoids));
    }

    private EvaluationData buildAutomaticEvaluationFromMl(SujetProjet projet,
                                                          EvaluationTrigger trigger,
                                                          String correlationId) {
        List<Livrable> livrables = livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(projet.getId());
        List<MlDeliverableContent> extractedContent = contentExtractionService.extract(livrables);
        List<MlExternalSourceRequest> externalSources = buildExternalSources(livrables);
        return buildAutomaticEvaluationFromMl(
                new ProjetContexte(
                        projet.getId(),
                        projet.getTitre(),
                        projet.getDomaine(),
                        projet.getTechnologies(),
                        projet.getCategorie() == null ? null : projet.getCategorie().name()),
                extractedContent,
                externalSources,
                trigger,
                correlationId
        );
    }

    private EvaluationData buildAutomaticEvaluationFromMlCatalogue(ProjetCatalogue projet,
                                                                   EvaluationTrigger trigger,
                                                                   String correlationId) {
        List<LivrableCatalogue> livrables = livrableCatalogueRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(projet.getId());
        List<MlDeliverableContent> extractedContent = contentExtractionService.extractCatalogue(livrables);
        List<MlExternalSourceRequest> externalSources = buildExternalSourcesCatalogue(livrables);
        return buildAutomaticEvaluationFromMl(
                new ProjetContexte(
                        projet.getId(),
                        projet.getTitre(),
                        joinDomaineNoms(projet),
                        joinTechnologieNoms(projet),
                        projet.getTypeProjet() == null ? null : projet.getTypeProjet().name()),
                extractedContent,
                externalSources,
                trigger,
                correlationId
        );
    }

    /**
     * Cœur du calcul ML, commun aux sujets et aux projets catalogue : ne dépend que des champs
     * projet (id, titre, domaine, technologies, catégorie) et des livrables déjà extraits.
     */
    private EvaluationData buildAutomaticEvaluationFromMl(ProjetContexte projet,
                                                          List<MlDeliverableContent> extractedContent,
                                                          List<MlExternalSourceRequest> externalSources,
                                                          EvaluationTrigger trigger,
                                                          String correlationId) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();
        Set<Integer> activeNoteValues = activeNoteValuesFor(criteresNotes);
        int maxActiveNoteValue = maxActiveNoteValue(activeNoteValues);
        CriteresActifs criteres = new CriteresActifs(criteresEliminatoires, criteresNotes, maxActiveNoteValue);
        String projectSnapshotHash = projectSnapshotHash(projet, criteresEliminatoires, criteresNotes);
        String deliverableSnapshotHash = deliverableSnapshotHash(extractedContent, externalSources);
        String inputSnapshotHash = sha256(projectSnapshotHash + ":" + deliverableSnapshotHash + ":" + criteriaVersion + ":" + evaluationEngine);
        SnapshotHashes hashes = new SnapshotHashes(projectSnapshotHash, deliverableSnapshotHash, inputSnapshotHash);
        MlEvaluationRequest mlRequest = buildMlRequest(
                projet,
                criteres,
                extractedContent,
                externalSources,
                correlationId,
                hashes
        );
        MlEvaluationResponse mlResponse = mlEvaluationClient.evaluate(
                mlRequest
        );
        validateMlResponse(mlResponse, criteresEliminatoires, criteresNotes, mlRequest, extractedContent);
        Map<Long, MlCriterionResult> mlResults = mlResponse.effectiveResults()
                .stream()
                .collect(Collectors.toMap(MlCriterionResult::criterionId, Function.identity()));

        List<ResultatCritere> resultats = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        double sommeScoresCriteres = 0.0;
        double sommePoids = 0.0;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            ResultatCritere resultat = baseResult(critere, TypeCritere.ELIMINATOIRE);
            MlCriterionResult mlResult = mlResults.get(critere.getId());
            String state = eliminatoryState(mlResult);
            ReponseEliminatoire resultatAuto = reponseEliminatoireDepuisEtat(state);
            resultat.setRuleConfigured(resultatAuto != null);
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
            Double normalized = usableNormalizedScore(mlResult);
            Integer noteValue = noteValueFromNormalized(normalized, maxActiveNoteValue, mlResult);
            double poids = positiveWeight(critere.getPoids());
            Double score = normalized == null ? null : normalized * poids;
            if (normalized != null) {
                sommeScoresCriteres += score;
                sommePoids += poids;
            }

            ResultatCritere resultat = baseResult(critere, TypeCritere.NOTE);
            resultat.setRuleConfigured(normalized != null);
            resultat.setModeEvaluation(ModeEvaluation.CONTENT_ANALYSIS);
            applyNoteResult(resultat, noteValue, poids, score, maxActiveNoteValue,
                    new TraceCalcul("ML_CONTENT_ANALYSIS", null, mlResult.justification()));
            applyMlResult(resultat, mlResult, evidenceSummary(mlResult), evidenceLivrableIds(mlResult));
            resultat.setCommentaire(mlResult.justification());
            resultats.add(resultat);
        }

        return buildEvaluationData(
                resultats,
                warnings,
                new ScoreAgregat(sommeScoresCriteres, sommePoids),
                mlResponse,
                trigger,
                correlationId,
                hashes
        );
    }

    private EvaluationData buildEvaluationData(List<ResultatCritere> resultats,
                                               List<String> eliminatoryWarnings,
                                               ScoreAgregat agregat) {
        return buildEvaluationData(resultats, eliminatoryWarnings, agregat, null, null, null, null);
    }

    private EvaluationData buildEvaluationData(List<ResultatCritere> resultats,
                                               List<String> eliminatoryWarnings,
                                               ScoreAgregat agregat,
                                               MlEvaluationResponse mlResponse,
                                               EvaluationTrigger trigger,
                                               String correlationId,
                                               SnapshotHashes hashes) {
        Double scoreFinal = agregat.sommePoids() > 0
                ? round((agregat.sommeScores() / agregat.sommePoids()) * 100)
                : null;
        boolean hasWarnings = !eliminatoryWarnings.isEmpty();
        String eligibilityStatus = eligibilityStatus(resultats, mlResponse, hasWarnings);
        boolean eligible = "ELIGIBLE".equals(eligibilityStatus);
        String commentaire = commentaireEvaluation(hasWarnings, eligibilityStatus);
        MlSynthese ml = mlSynthese(mlResponse);
        SnapshotHashes empreintes = hashes == null ? new SnapshotHashes(null, null, null) : hashes;
        return new EvaluationData(
                resultats,
                scoreFinal,
                eligible,
                hasWarnings,
                eliminatoryWarnings,
                commentaire,
                ml.statut(),
                trigger == null ? null : trigger.name(),
                correlationId,
                ml.engineType(),
                ml.modelName(),
                ml.modelVersion(),
                ml.pipelineVersion(),
                ml.criteriaVersion(),
                empreintes.projet(),
                empreintes.livrables(),
                empreintes.input(),
                ml.effectiveStatus(),
                eligibilityStatus,
                ml.globalConfidence(),
                scoreFinal,
                ml.warnings(),
                ml.strengths(),
                ml.weaknesses(),
                ml.recommendations(),
                null
        );
    }

    /**
     * Valeurs derivees de la reponse ML, ramenees a des valeurs neutres lorsque l'evaluation
     * est manuelle (aucune reponse du moteur).
     */
    private MlSynthese mlSynthese(MlEvaluationResponse mlResponse) {
        if (mlResponse == null) {
            return new MlSynthese("MANUAL", null, null, null, null, criteriaVersion, "MANUAL", null,
                    List.of(), List.of(), List.of(), List.of());
        }
        String version = StringUtils.hasText(mlResponse.criteriaVersion())
                ? mlResponse.criteriaVersion()
                : criteriaVersion;
        return new MlSynthese(
                safeValue(mlResponse.effectiveStatus()),
                mlResponse.engineType(),
                mlResponse.modelName(),
                mlResponse.modelVersion(),
                safeValue(mlResponse.effectivePipelineVersion()),
                version,
                mlResponse.effectiveStatus(),
                mlResponse.globalConfidence(),
                safeList(mlResponse.warnings()),
                safeList(mlResponse.strengths()),
                safeList(mlResponse.weaknesses()),
                safeList(mlResponse.recommendations()));
    }

    private EvaluationResponse persistEvaluation(SujetProjet projet,
                                                 EvaluationData data,
                                                 String calculatedBy,
                                                 String recalculationReason) {
        Evaluation evaluation = new Evaluation();
        evaluation.setSujetProjet(projet);
        evaluation.setProjetCatalogue(null);
        evaluation.setScoreFinal(data.scoreFinal());
        evaluation.setMlScore(data.mlScore());
        evaluation.setMlStatus(data.mlStatus());
        evaluation.setTrigger(data.trigger());
        evaluation.setCorrelationId(data.correlationId());
        evaluation.setEngineType(data.engineType());
        evaluation.setModelName(data.modelName());
        evaluation.setMlModelVersion(data.mlModelVersion());
        evaluation.setPipelineVersion(data.pipelineVersion());
        evaluation.setCriteriaVersion(data.criteriaVersion());
        evaluation.setProjectSnapshotHash(data.projectSnapshotHash());
        evaluation.setDeliverableSnapshotHash(data.deliverableSnapshotHash());
        evaluation.setInputSnapshotHash(data.inputSnapshotHash());
        evaluation.setProcessingStatus(data.processingStatus());
        evaluation.setEligibilityStatus(data.eligibilityStatus());
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
        validateEvaluationParent(evaluation);

        Evaluation savedEvaluation = saveEvaluationWithResults(evaluation, data.resultats());

        projet.setScoreFinalEvaluation(data.scoreFinal());
        projet.setEligibleIndustrialisation(Boolean.TRUE.equals(data.eligibleIndustrialisation()));
        projet.setBloqueParEliminatoire(data.hasEliminatoryWarnings());
        projet.setHasEliminatoryWarnings(data.hasEliminatoryWarnings());
        projet.setEliminatoryWarningsCount(data.eliminatoryWarnings().size());
        sujetProjetRepository.save(projet);

        if (data.scoreFinal() != null) {
            projetCatalogueService.mettreAJourScoreParSujetId(
                    projet.getId(),
                    (int) Math.round(data.scoreFinal())
            );
        }
        if (projet.getEncadrantId() != null && data.scoreFinal() != null) {
            notificationService.creerNotification(
                    projet.getEncadrantId(),
                    NotificationType.EVALUATION_CALCULEE,
                    "Evaluation calculee",
                    "L'evaluation automatique du projet \"" + projet.getTitre() + "\" est terminee. Score : " + String.format("%.1f", data.scoreFinal()) + SUFFIXE_SUR_CENT,
                    LIEN_MES_PROJETS);
        }

        return toResponse(savedEvaluation);
    }

    private EvaluationResponse persistEvaluationCatalogue(ProjetCatalogue projet,
                                                         EvaluationData data,
                                                         String calculatedBy,
                                                         String recalculationReason) {
        Evaluation evaluation = new Evaluation();
        evaluation.setSujetProjet(null);
        evaluation.setProjetCatalogue(projet);
        evaluation.setScoreFinal(data.scoreFinal());
        evaluation.setMlScore(data.mlScore());
        evaluation.setMlStatus(data.mlStatus());
        evaluation.setTrigger(data.trigger());
        evaluation.setCorrelationId(data.correlationId());
        evaluation.setEngineType(data.engineType());
        evaluation.setModelName(data.modelName());
        evaluation.setMlModelVersion(data.mlModelVersion());
        evaluation.setPipelineVersion(data.pipelineVersion());
        evaluation.setCriteriaVersion(data.criteriaVersion());
        evaluation.setProjectSnapshotHash(data.projectSnapshotHash());
        evaluation.setDeliverableSnapshotHash(data.deliverableSnapshotHash());
        evaluation.setInputSnapshotHash(data.inputSnapshotHash());
        evaluation.setProcessingStatus(data.processingStatus());
        evaluation.setEligibilityStatus(data.eligibilityStatus());
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
        validateEvaluationParent(evaluation);

        Evaluation savedEvaluation = saveEvaluationWithResults(evaluation, data.resultats());

        if (data.scoreFinal() != null) {
            projet.setScore(Math.max(0, (int) Math.round(data.scoreFinal())));
            projetCatalogueRepository.save(projet);
        }

        return toResponse(savedEvaluation);
    }

    void validateEvaluationParent(Evaluation evaluation) {
        boolean hasSujetProjet = evaluation.getSujetProjet() != null;
        boolean hasProjetCatalogue = evaluation.getProjetCatalogue() != null;
        if (hasSujetProjet == hasProjetCatalogue) {
            throw new BusinessException(
                    "Une evaluation doit etre rattachee exactement a un SujetProjet ou a un ProjetCatalogue."
            );
        }
    }

    private Evaluation saveEvaluationWithResults(Evaluation evaluation, List<ResultatCritere> resultats) {
        try {
            Evaluation savedEvaluation = evaluationRepository.saveAndFlush(evaluation);
            safeList(resultats).forEach(savedEvaluation::addResultat);
            return evaluationRepository.saveAndFlush(savedEvaluation);
        } catch (RuntimeException ex) {
            throw new EvaluationPersistenceException(
                    "L'evaluation a ete calculee mais n'a pas pu etre enregistree.",
                    ex
            );
        }
    }

    private void applyNoteResult(ResultatCritere resultat,
                                 Integer noteValue,
                                 Double poids,
                                 Double scoreCritere,
                                 int maxActiveNoteValue,
                                 TraceCalcul trace) {
        resultat.setNoteValue(noteValue);
        resultat.setNoteLabel(libelleNote(noteValue));
        resultat.setNoteObtenue(noteValue == null ? null : noteValue.doubleValue());
        resultat.setBareme((double) maxActiveNoteValue);
        resultat.setPoids(poids);
        resultat.setScorePondere(scoreCritere == null ? null : round(scoreCritere));
        resultat.setMatchedRule(trace.matchedRule());
        resultat.setMetadataUsed(trace.metadataUsed());
        resultat.setExplanation(trace.explanation());
        resultat.setEvidenceSummary(trace.metadataUsed());
    }

    private MlEvaluationRequest buildMlRequest(ProjetContexte projet,
                                               CriteresActifs criteres,
                                               List<MlDeliverableContent> extractedContent,
                                               List<MlExternalSourceRequest> externalSources,
                                               String correlationId,
                                               SnapshotHashes hashes) {
        List<MlCriterionRequest> criteria = new ArrayList<>();

        for (CritereEliminatoire critere : criteres.eliminatoires()) {
            criteria.add(buildMlCriterionRequest(
                    critere,
                    TypeCritere.ELIMINATOIRE,
                    1.0,
                    0.0,
                    true
            ));
        }

        for (CritereNote critere : criteres.notes()) {
            double maximumScore = positiveMaximumScore(critere.getBareme(), criteres.maxNoteValue());
            criteria.add(buildMlCriterionRequest(
                    critere,
                    TypeCritere.NOTE,
                    maximumScore,
                    positiveWeight(critere.getPoids()),
                    false
            ));
        }

        return new MlEvaluationRequest(
                projet.id(),
                correlationId,
                criteriaVersion,
                evaluationEngine,
                hashes.input(),
                hashes.projet(),
                hashes.livrables(),
                new MlProjectContext(
                        projet.id(),
                        safeValue(projet.titre()),
                        safeValue(projet.domaine()),
                        safeValue(projet.technologies()),
                        projet.categorieName()
                ),
                criteria,
                extractedContent,
                externalSources
        );
    }

    private MlCriterionRequest buildMlCriterionRequest(CritereEvaluation critere,
                                                       TypeCritere typeCritere,
                                                       double maximumScore,
                                                       double weight,
                                                       boolean eliminatory) {
        List<String> requiredEvidenceTypes = requiredEvidenceTypes(critere);
        List<String> optionalEvidenceTypes = splitConfigurationValues(critere.getOptionalEvidenceTypes());
        List<String> positiveExpectations = splitConfigurationValues(critere.getPositiveEvidenceExpectations());
        List<String> negativeExpectations = splitConfigurationValues(critere.getNegativeEvidenceExpectations());
        List<String> permittedNatures = splitConfigurationValues(critere.getPermittedEvidenceNatures());

        int minimumEvidence = critere.getMinimumSupportingEvidenceCount() == null
                || critere.getMinimumSupportingEvidenceCount() <= 0
                ? 1
                : critere.getMinimumSupportingEvidenceCount();

        MlCriterionRequest mlCriterion = new MlCriterionRequest(
                critere.getId(),
                typeCritere.name(),
                critere.getLibelle(),
                critere.getLibelle(),
                critere.getDescription(),
                critere.getDomaine(),
                maximumScore,
                maximumScore,
                weight,
                eliminatory,
                criterionVersion(critere),
                requiredEvidenceTypes,
                critere.getCriterionFamily(),
                critere.getApplicability(),
                critere.getConfigurationVersion(),
                positiveExpectations,
                negativeExpectations,
                permittedNatures,
                minimumEvidence,
                critere.getScoringPolicy(),
                critere.getEliminatoryDecisionRule(),
                true
        );

        log.info(
                "ML_CRITERION_CONFIG id={} libelle='{}' type={} family={} required={} optional={} natures={} policy={} minEvidence={} positiveCount={} negativeCount={} applicability={} configVersion={}",
                critere.getId(),
                critere.getLibelle(),
                typeCritere,
                critere.getCriterionFamily(),
                requiredEvidenceTypes,
                optionalEvidenceTypes,
                permittedNatures,
                critere.getScoringPolicy(),
                minimumEvidence,
                positiveExpectations.size(),
                negativeExpectations.size(),
                critere.getApplicability(),
                criterionVersion(critere)
        );

        if (!optionalEvidenceTypes.isEmpty()) {
            log.info(
                    "ML_CRITERION_OPTIONAL_EVIDENCE id={} optionalEvidenceTypes={} note='Current MlCriterionRequest constructor does not expose optionalEvidenceTypes; verify DTO if FastAPI requires this field.'",
                    critere.getId(),
                    optionalEvidenceTypes
            );
        }

        return mlCriterion;
    }

    private List<MlExternalSourceRequest> buildExternalSources(List<Livrable> livrables) {
        return livrables.stream()
                .filter(livrable -> livrable.getTypeLivrable() == tn.esprit.backend_esprittech.entity.livrable.TypeLivrable.LIEN_GIT)
                .filter(livrable -> StringUtils.hasText(livrable.getLienExterne()))
                .map(livrable -> new MlExternalSourceRequest(
                        "GIT",
                        livrable.getLienExterne().trim(),
                        null,
                        List.of(),
                        null
                ))
                .toList();
    }

    private List<MlExternalSourceRequest> buildExternalSourcesCatalogue(List<LivrableCatalogue> livrables) {
        return livrables.stream()
                .filter(livrable -> livrable.getTypeLivrable() == tn.esprit.backend_esprittech.entity.livrable.TypeLivrable.LIEN_GIT)
                .filter(livrable -> StringUtils.hasText(livrable.getLienExterne()))
                .map(livrable -> new MlExternalSourceRequest(
                        "GIT",
                        livrable.getLienExterne().trim(),
                        null,
                        List.of(),
                        null
                ))
                .toList();
    }

    private String joinDomaineNoms(ProjetCatalogue projet) {
        return projet.getDomaines().stream()
                .map(SujetDomaine::getNom)
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private String joinTechnologieNoms(ProjetCatalogue projet) {
        return projet.getTechnologies().stream()
                .map(SujetTechnologie::getNom)
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private List<String> requiredEvidenceTypes(CritereEvaluation critere) {
        if (StringUtils.hasText(critere.getRequiredEvidenceTypes())) {
            return splitConfigurationValues(critere.getRequiredEvidenceTypes());
        }
        String source = safeValue(critere.getLibelle()) + " "
                + safeValue(critere.getDescription()) + " "
                + safeValue(critere.getDomaine()) + " "
                + safeValue(critere.getExpectedLivrableTypes());
        String normalized = source.toLowerCase();
        List<String> required = new ArrayList<>();
        if (normalized.contains("git") || normalized.contains("code source") || normalized.contains("repository")) {
            required.add("GIT");
        }
        if (normalized.contains("sonar") || normalized.contains("secur") || normalized.contains("security")) {
            required.add("SONAR");
        }
        if (normalized.contains("coverage") || normalized.contains("couverture")) {
            required.add("COVERAGE");
        }
        if (normalized.contains("image") || normalized.contains("diagram") || normalized.contains("screenshot")) {
            required.add("IMAGE");
        }
        if (required.isEmpty()) {
            required.add("DOCUMENT");
        }
        return required.stream().distinct().toList();
    }

    private String criterionVersion(CritereEvaluation critere) {
        return StringUtils.hasText(critere.getConfigurationVersion())
                ? critere.getConfigurationVersion()
                : criteriaVersion;
    }

    private List<String> splitConfigurationValues(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }

        String trimmed = value.trim();

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {})
                        .stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();
            } catch (Exception ex) {
                log.warn("ML_CONFIG_PARSE_FALLBACK value={} reason={}", trimmed, ex.getMessage());
            }
        }

        return Arrays.stream(trimmed.split("[,;\\n]+"))
                .map(String::trim)
                .map(token -> token.replace("[", ""))
                .map(token -> token.replace("]", ""))
                .map(token -> token.replace("\"", ""))
                .map(token -> token.replace("'", ""))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String projectSnapshotHash(ProjetContexte projet,
                                       List<CritereEliminatoire> criteresEliminatoires,
                                       List<CritereNote> criteresNotes) {
        List<CritereEvaluation> allCriteria = new ArrayList<>();
        allCriteria.addAll(criteresEliminatoires);
        allCriteria.addAll(criteresNotes);
        String criteria = allCriteria.stream()
                .sorted(Comparator.comparing(CritereEvaluation::getId))
                .map(critere -> critere.getId() + "|" + safeValue(critere.getLibelle()) + "|"
                        + safeValue(critere.getDescription()) + "|" + safeValue(critere.getDomaine())
                        + "|" + safeValue(critere.getExpectedLivrableTypes())
                        + "|" + safeValue(critere.getCriterionFamily())
                        + "|" + safeValue(critere.getConfigurationVersion())
                        + "|" + safeValue(critere.getRequiredEvidenceTypes())
                        + "|" + safeValue(critere.getOptionalEvidenceTypes())
                        + "|" + safeValue(critere.getPermittedEvidenceNatures())
                        + "|" + safeValue(critere.getScoringPolicy())
                        + "|" + safeValue(critere.getPositiveEvidenceExpectations())
                        + "|" + safeValue(critere.getNegativeEvidenceExpectations())
                        + "|" + safeValue(critere.getSourceAuthorityRules()))
                .collect(Collectors.joining(";"));
        return sha256(projet.id() + "|" + safeValue(projet.titre()) + "|"
                + safeValue(projet.domaine()) + "|" + safeValue(projet.technologies())
                + "|" + safeValue(projet.categorieName())
                + "|" + criteriaVersion + "|" + criteria);
    }

    private String deliverableSnapshotHash(List<MlDeliverableContent> deliverables,
                                           List<MlExternalSourceRequest> externalSources) {
        String deliverablePart = deliverables.stream()
                .sorted(Comparator.comparing(MlDeliverableContent::deliverableId, Comparator.nullsLast(Long::compareTo)))
                .map(deliverable -> deliverable.deliverableId() + "|" + safeValue(deliverable.filename())
                        + "|" + safeValue(deliverable.sourceType()) + "|" + safeValue(deliverable.contentHash())
                        + "|" + safeValue(deliverable.extractionStatus()))
                .collect(Collectors.joining(";"));
        String externalPart = externalSources.stream()
                .sorted(Comparator.comparing(MlExternalSourceRequest::reference))
                .map(source -> safeValue(source.sourceType()) + "|" + safeValue(source.reference()))
                .collect(Collectors.joining(";"));
        return sha256(deliverablePart + "|" + externalPart);
    }

    private double positiveMaximumScore(Double bareme, int maxActiveNoteValue) {
        if (bareme != null && bareme > 0.0) {
            return bareme;
        }
        return maxActiveNoteValue > 0 ? maxActiveNoteValue : 1.0;
    }

    private void validateMlResponse(MlEvaluationResponse response,
                                    List<CritereEliminatoire> criteresEliminatoires,
                                    List<CritereNote> criteresNotes,
                                    MlEvaluationRequest request,
                                    List<MlDeliverableContent> deliverables) {
        validateResponseEnvelope(response, request);

        Set<Long> eliminatoryIds = new HashSet<>();
        criteresEliminatoires.forEach(critere -> eliminatoryIds.add(critere.getId()));
        validateCriterionCoverage(response, criteresEliminatoires, criteresNotes);

        Set<Long> deliverableIds = deliverables.stream()
                .map(MlDeliverableContent::deliverableId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        response.effectiveResults()
                .forEach(result -> validateCriterionResult(result, eliminatoryIds, deliverableIds));
    }

    /** Coherence de l'enveloppe : statut, correlation, snapshot et version de pipeline. */
    private void validateResponseEnvelope(MlEvaluationResponse response, MlEvaluationRequest request) {
        if (response == null || response.effectiveResults() == null) {
            throw new BusinessException("Le moteur ML a retourne une reponse vide.");
        }
        if (!StringUtils.hasText(response.effectiveStatus())) {
            throw new BusinessException("La reponse ML ne contient pas de statut.");
        }
        if (StringUtils.hasText(request.correlationId())
                && StringUtils.hasText(response.correlationId())
                && !request.correlationId().equals(response.correlationId())) {
            throw new BusinessException("La correlation de la reponse ML est invalide.");
        }
        if (StringUtils.hasText(request.inputSnapshotHash())
                && StringUtils.hasText(response.inputSnapshotHash())
                && !request.inputSnapshotHash().equals(response.inputSnapshotHash())) {
            throw new BusinessException("La reponse ML ne correspond pas au snapshot demande.");
        }
        if (VERSION_MOTEUR_ML.equals(request.engineVersion())
                && !VERSION_MOTEUR_ML.equals(response.effectivePipelineVersion())) {
            throw new BusinessException("La version de pipeline ML est invalide.");
        }
    }

    /** Chaque critere actif doit etre renvoye exactement une fois, et aucun critere inconnu. */
    private void validateCriterionCoverage(MlEvaluationResponse response,
                                           List<CritereEliminatoire> criteresEliminatoires,
                                           List<CritereNote> criteresNotes) {
        Set<Long> expectedIds = new HashSet<>();
        criteresEliminatoires.forEach(critere -> expectedIds.add(critere.getId()));
        criteresNotes.forEach(critere -> expectedIds.add(critere.getId()));

        Set<Long> returnedIds = new HashSet<>();
        for (MlCriterionResult result : response.effectiveResults()) {
            if (result.criterionId() == null) {
                throw new BusinessException("La reponse ML contient un critere sans identifiant.");
            }
            if (!returnedIds.add(result.criterionId())) {
                throw new BusinessException("La reponse ML contient un critere en double: " + result.criterionId());
            }
            if (!expectedIds.contains(result.criterionId())) {
                throw new BusinessException("La reponse ML contient un critere inconnu: " + result.criterionId());
            }
        }
        if (!returnedIds.equals(expectedIds)) {
            throw new BusinessException("La reponse ML ne couvre pas tous les criteres actifs.");
        }
    }

    /** Scores, bornes et preuves d'un critere renvoye par le moteur ML. */
    private void validateCriterionResult(MlCriterionResult result,
                                         Set<Long> eliminatoryIds,
                                         Set<Long> deliverableIds) {
        validateCriterionScores(result);
        for (MlEvidenceResponse evidence : safeList(result.evidence())) {
            validateEvidence(evidence, result.criterionId(), deliverableIds);
        }
        if (eliminatoryIds.contains(result.criterionId())
                && STATUT_NOT_OK.equals(safeUpper(result.eliminatoryState()))
                && !hasConfirmedNegativeEvidence(result)) {
            throw new BusinessException("La reponse ML declare NOT_OK sans preuve negative confirmee pour le critere " + result.criterionId());
        }
        if (result.confidence() == null || result.confidence() < 0.0 || result.confidence() > 1.0) {
            throw new BusinessException("La confiance ML est invalide pour le critere " + result.criterionId());
        }
    }

    /** Presence et domaine de definition des metriques numeriques du critere. */
    private void validateCriterionScores(MlCriterionResult result) {
        boolean scoreMissingAllowed = Set.of(
                        STATUT_INSUFFICIENT_EVIDENCE,
                        STATUT_INDETERMINATE,
                        "MODEL_UNAVAILABLE",
                        STATUT_FAILED_RETRYABLE,
                        STATUT_CONFIGURATION_INCOMPLETE
                )
                .contains(safeUpper(result.status()));
        if (scoreMissingAllowed && (result.normalizedScore() != null || result.score() != null)) {
            throw new BusinessException("La reponse ML contient un score non nul pour un critere non evaluable " + result.criterionId());
        }
        if (result.normalizedScore() == null && !scoreMissingAllowed) {
            throw new BusinessException("La reponse ML contient un score manquant pour le critere " + result.criterionId());
        }
        validateRange(result.normalizedScore(), "score normalise ML", result.criterionId(), true);
        validateRange(result.confidence(), "confiance ML", result.criterionId(), false);
        validateRange(result.evidenceQuality(), "qualite preuve ML", result.criterionId(), true);
        validateRange(result.extractionQuality(), "qualite extraction ML", result.criterionId(), true);
        validateRange(result.sourceCoverage(), "couverture source ML", result.criterionId(), true);
        validateRange(result.supportStrength(), "force de support ML", result.criterionId(), true);
        validateRange(result.contradictionStrength(), "force contradiction ML", result.criterionId(), true);
        validateRange(result.projectSpecificity(), "specificite projet ML", result.criterionId(), true);
        validateRange(result.stanceConfidence(), "confiance stance ML", result.criterionId(), true);
        if (result.score() != null && !finite(result.score())) {
            throw new BusinessException("La reponse ML contient un score non fini pour le critere " + result.criterionId());
        }
        if (result.maxScore() != null && (!finite(result.maxScore()) || result.maxScore() < 0.0)) {
            throw new BusinessException("La reponse ML contient un bareme invalide pour le critere " + result.criterionId());
        }
    }

    /** Une preuve doit citer un livrable connu, rester dans les bornes et porter un extrait. */
    private void validateEvidence(MlEvidenceResponse evidence, Long criterionId, Set<Long> deliverableIds) {
        if (evidence.deliverableId() != null && !deliverableIds.contains(evidence.deliverableId())) {
            throw new BusinessException("La reponse ML reference un livrable inconnu: " + evidence.deliverableId());
        }
        validateRange(evidence.relevance(), "pertinence preuve ML", criterionId, false);
        validateRange(evidence.rawSemanticSimilarity(), "similarite brute preuve ML", criterionId, true);
        validateRange(evidence.calibratedRelevance(), "pertinence calibree preuve ML", criterionId, true);
        validateRange(evidence.projectRelevanceScore(), "pertinence projet preuve ML", criterionId, true);
        validateRange(evidence.rankingMargin(), "marge classement preuve ML", criterionId, true);
        validateRange(evidence.stanceConfidence(), "confiance stance preuve ML", criterionId, true);
        validateRange(evidence.sourceAuthority(), "autorite source preuve ML", criterionId, true);
        validateRange(evidence.contradictionStrength(), "force contradiction preuve ML", criterionId, true);
        validateRange(evidence.evidenceQuality(), "qualite preuve detail ML", criterionId, true);
        validateRange(evidence.extractionQuality(), "qualite extraction preuve ML", criterionId, true);
        validateRange(evidence.documentCategoryConfidence(), "confiance categorie document ML", criterionId, true);
        validateAllowedEvidenceState(evidence, criterionId);
        if (!StringUtils.hasText(evidence.excerpt())) {
            throw new BusinessException("La reponse ML contient une preuve vide pour le critere " + criterionId);
        }
    }

    private void applyMlResult(ResultatCritere resultat,
                               MlCriterionResult mlResult,
                               String evidenceSummary,
                               String evidenceLivrableIds) {
        resultat.setMlScore(mlResult.score());
        resultat.setMlMaxScore(mlResult.maxScore());
        resultat.setNormalizedScore(mlResult.normalizedScore());
        resultat.setConfidence(mlResult.confidence());
        resultat.setCriterionStatus(mlResult.status());
        resultat.setEvidenceQuality(mlResult.evidenceQuality());
        resultat.setExtractionQuality(mlResult.extractionQuality());
        resultat.setSourceCoverage(mlResult.sourceCoverage());
        resultat.setEliminatoryState(mlResult.eliminatoryState());
        resultat.setAnalysisMethods(joinLines(safeList(mlResult.analysisMethods())));
        resultat.setEvidenceJson(evidenceDetails(mlResult));
        resultat.setExplanation(mlResult.justification());
        resultat.setEvidenceSummary(evidenceSummary);
        resultat.setEvidenceLivrableIds(evidenceLivrableIds);
        resultat.setStrengths(joinLines(safeList(mlResult.strengths())));
        resultat.setWeaknesses(joinLines(criterionWeaknesses(mlResult)));
        resultat.setRecommendations(joinLines(safeList(mlResult.recommendations())));
        resultat.setMetadataUsed(null);
    }

    private List<String> criterionWeaknesses(MlCriterionResult mlResult) {
        List<String> values = new ArrayList<>(safeList(mlResult.weaknesses()));
        if (STATUT_CONFIGURATION_INCOMPLETE.equals(safeUpper(mlResult.status()))) {
            values.addAll(safeList(mlResult.reasonCodes()));
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private Double usableNormalizedScore(MlCriterionResult mlResult) {
        if (mlResult.normalizedScore() != null) {
            return clamp01(mlResult.normalizedScore());
        }
        if (Set.of(STATUT_INSUFFICIENT_EVIDENCE, STATUT_INDETERMINATE).contains(safeUpper(mlResult.status()))) {
            return null;
        }
        double maxScore = nullToZero(mlResult.maxScore());
        if (maxScore <= 0.0) {
            return null;
        }
        return clamp01(nullToZero(mlResult.score()) / maxScore);
    }

    private Integer noteValueFromNormalized(Double normalized, int maxActiveNoteValue, MlCriterionResult mlResult) {
        if (normalized == null) {
            return null;
        }
        String status = safeUpper(mlResult == null ? null : mlResult.status());
        if (Set.of(STATUT_INSUFFICIENT_EVIDENCE, STATUT_INDETERMINATE, "MODEL_UNAVAILABLE", STATUT_FAILED_RETRYABLE,
                        "FAILED_PERMANENT", "NOT_EVALUABLE_NO_DELIVERABLE", STATUT_CONFIGURATION_INCOMPLETE)
                .contains(status)) {
            return null;
        }
        if (maxActiveNoteValue <= 0) {
            return null;
        }
        if (normalized <= 0.0) {
            return "SCORED".equals(status) ? 0 : null;
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

    private String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean finite(Double value) {
        return value != null && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private void validateRange(Double value,
                               String label,
                               Long criterionId,
                               boolean nullable) {
        if (value == null) {
            if (!nullable) {
                throw new BusinessException("La reponse ML contient une valeur manquante pour " + label + " critere " + criterionId);
            }
            return;
        }
        if (!finite(value) || value < 0.0 || value > 1.0) {
            throw new BusinessException("La reponse ML contient une valeur invalide pour " + label + " critere " + criterionId);
        }
    }

    private void validateAllowedEvidenceState(MlEvidenceResponse evidence, Long criterionId) {
        String stance = safeUpper(evidence.stance());
        if (StringUtils.hasText(stance)
                && !Set.of("SUPPORTS", "CONTRADICTS", "NEUTRAL", "INSUFFICIENT").contains(stance)) {
            throw new BusinessException("La reponse ML contient une stance inconnue pour le critere " + criterionId);
        }
        String projectRelevance = safeUpper(evidence.projectRelevanceType());
        if (StringUtils.hasText(projectRelevance)
                && !Set.of(
                "PROJECT_SPECIFIC",
                "GENERIC_DOMAIN_CONTENT",
                "EVALUATOR_DOCUMENTATION",
                "EXAMPLE_OR_TEMPLATE",
                "HISTORICAL_CONTENT",
                "UNKNOWN"
        ).contains(projectRelevance)) {
            throw new BusinessException("La reponse ML contient une pertinence projet inconnue pour le critere " + criterionId);
        }
        String documentCategory = safeUpper(evidence.documentCategory());
        if (StringUtils.hasText(documentCategory)
                && !Set.of(
                "PROJECT_DELIVERABLE",
                "EVALUATOR_DOCUMENTATION",
                "GENERIC_REFERENCE",
                "TEMPLATE_OR_EXAMPLE",
                "UNKNOWN_DOCUMENT"
        ).contains(documentCategory)) {
            throw new BusinessException("La reponse ML contient une categorie documentaire inconnue pour le critere " + criterionId);
        }
        String evidenceNature = safeUpper(evidence.evidenceNature());
        if (StringUtils.hasText(evidenceNature)
                && !Set.of("SEMANTIC", "DETERMINISTIC", "STRUCTURED_REPORT").contains(evidenceNature)) {
            throw new BusinessException("La reponse ML contient une nature de preuve inconnue pour le critere " + criterionId);
        }
        if ("SUPPORTS".equals(stance)
                && !"PROJECT_SPECIFIC".equals(projectRelevance)
                && !Set.of("DETERMINISTIC", "STRUCTURED_REPORT").contains(evidenceNature)) {
            throw new BusinessException("La reponse ML declare SUPPORTS sans pertinence projet confirmee pour le critere " + criterionId);
        }
    }

    private boolean hasConfirmedNegativeEvidence(MlCriterionResult result) {
        boolean evidenceContradicts = safeList(result.evidence()).stream()
                .anyMatch(evidence -> "CONTRADICTS".equals(safeUpper(evidence.stance()))
                        && nullToZero(evidence.contradictionStrength()) > 0.0);
        boolean reasonConfirms = safeList(result.reasonCodes()).stream()
                .map(this::safeUpper)
                .anyMatch(reason -> Set.of(
                        "CONFIRMED_NEGATIVE_EVIDENCE",
                        "CONFIRMED_SECURITY_FINDINGS"
                ).contains(reason));
        return evidenceContradicts || reasonConfirms;
    }

    /** Reponse eliminatoire deduite de l'etat ML, ou {@code null} si l'etat n'est pas concluant. */
    private ReponseEliminatoire reponseEliminatoireDepuisEtat(String state) {
        if ("OK".equals(state)) {
            return ReponseEliminatoire.OK;
        }
        if (STATUT_NOT_OK.equals(state)) {
            return ReponseEliminatoire.NOT_OK;
        }
        return null;
    }

    /** Commentaire de synthese affiche a l'utilisateur selon l'issue de l'evaluation. */
    private String commentaireEvaluation(boolean hasWarnings, String eligibilityStatus) {
        if (hasWarnings) {
            return "Projet evalue avec alertes eliminatoires non bloquantes.";
        }
        if ("NOT_EVALUABLE".equals(eligibilityStatus)) {
            return "Evaluation impossible avec les informations disponibles.";
        }
        if ("REVIEW_REQUIRED".equals(eligibilityStatus)) {
            return "Projet evalue avec elements indetermines a revoir.";
        }
        return "Projet evalue avec succes.";
    }

    /** Origine de l'evaluation : sujet de projet, projet du catalogue, ou {@code null} si aucune. */
    private String contexteEvaluation(Long sujetProjetId, Long projetCatalogueId) {
        if (sujetProjetId != null) {
            return "SUJET_PROJET";
        }
        if (projetCatalogueId != null) {
            return "PROJET_CATALOGUE";
        }
        return null;
    }

    /** Libelle du niveau de note : la valeur 0 marque un critere non evaluable. */
    private String libelleNote(Integer noteValue) {
        if (noteValue == null) {
            return "Non calculé";
        }
        if (noteValue == 0) {
            return "Non évaluable";
        }
        return noteLevelService.labelFor(noteValue);
    }

    private String eliminatoryState(MlCriterionResult mlResult) {
        String state = safeUpper(mlResult.eliminatoryState());
        if (Set.of("OK", STATUT_NOT_OK, STATUT_INDETERMINATE, "NOT_APPLICABLE").contains(state)) {
            return state;
        }
        if (Boolean.TRUE.equals(mlResult.satisfied())) {
            return "OK";
        }
        if (Boolean.FALSE.equals(mlResult.satisfied())) {
            return STATUT_NOT_OK;
        }
        String status = safeUpper(mlResult.status());
        if (Set.of("OK", STATUT_NOT_OK, STATUT_INDETERMINATE).contains(status)) {
            return status;
        }
        return STATUT_INDETERMINATE;
    }

    private String eligibilityStatus(List<ResultatCritere> resultats,
                                     MlEvaluationResponse mlResponse,
                                     boolean hasConfirmedFailure) {
        if (mlResponse != null && Set.of(STATUT_FAILED_RETRYABLE, "FAILED_PERMANENT", "NOT_EVALUABLE_NO_DELIVERABLE")
                .contains(safeUpper(mlResponse.effectiveStatus()))) {
            return "NOT_EVALUABLE";
        }
        if (hasConfirmedFailure) {
            return "NON_ELIGIBLE_EN_L_ETAT";
        }
        boolean indeterminate = resultats.stream()
                .filter(resultat -> resultat.getTypeCritere() == TypeCritere.ELIMINATOIRE)
                .anyMatch(resultat -> resultat.getReponseEliminatoire() == null
                        || STATUT_INDETERMINATE.equals(safeUpper(resultat.getEliminatoryState())));
        return indeterminate ? "REVIEW_REQUIRED" : "ELIGIBLE";
    }

    private String evidenceDetails(MlCriterionResult mlResult) {
        List<MlEvidenceResponse> evidence = safeList(mlResult.evidence());
        if (evidence.isEmpty()) {
            return null;
        }
        return evidence.stream()
                .limit(8)
                .map(this::evidenceJson)
                .collect(Collectors.joining(",", "[", "]"));
    }

    /** Serialise une preuve ML en JSON compact. */
    private String evidenceJson(MlEvidenceResponse item) {
        String reasonCodes = safeList(item.documentCategoryReasonCodes()).stream()
                .map(reason -> "\"" + jsonEscape(reason) + "\"")
                .collect(Collectors.joining(","));
        return "{" + String.join(",",
                jsonText("evidenceId", item.evidenceId()),
                jsonNumber("deliverableId", item.deliverableId()),
                jsonText("sourceType", item.sourceType()),
                jsonText("source", item.source()),
                jsonNumber("page", item.page()),
                jsonText("path", item.path()),
                jsonText("contentHash", item.contentHash()),
                jsonNumber("relevance", item.relevance()),
                jsonNumber("rawSemanticSimilarity", item.rawSemanticSimilarity()),
                jsonNumber("calibratedRelevance", item.calibratedRelevance()),
                jsonNumber("projectRelevanceScore", item.projectRelevanceScore()),
                jsonNumber("rankingMargin", item.rankingMargin()),
                jsonText("stance", item.stance()),
                jsonText("projectRelevanceType", item.projectRelevanceType()),
                jsonNumber("stanceConfidence", item.stanceConfidence()),
                jsonNumber("sourceAuthority", item.sourceAuthority()),
                jsonNumber("contradictionStrength", item.contradictionStrength()),
                jsonNumber("evidenceQuality", item.evidenceQuality()),
                jsonNumber("extractionQuality", item.extractionQuality()),
                jsonText("documentCategory", item.documentCategory()),
                jsonNumber("documentCategoryConfidence", item.documentCategoryConfidence()),
                "\"documentCategoryReasonCodes\":[" + reasonCodes + "]",
                jsonText("evidenceNature", item.evidenceNature()),
                jsonText("excerpt", item.excerpt())
        ) + "}";
    }

    /** Champ JSON textuel, valeur echappee. */
    private String jsonText(String name, String value) {
        return "\"" + name + "\":\"" + jsonEscape(value) + "\"";
    }

    /** Champ JSON numerique ; le litteral null est emis lorsque la valeur est absente. */
    private String jsonNumber(String name, Object value) {
        return "\"" + name + "\":" + (value == null ? "null" : value);
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new BusinessException("Impossible de calculer le hash du snapshot.");
        }
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
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private List<String> splitLines(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("\\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
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
        Evaluation latest = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDescIdDesc(projetId).orElse(null);
        if (latest == null || latest.getDateCalcul() == null) {
            return;
        }
        LocalDateTime allowedAt = latest.getDateCalcul().plusSeconds(recalculationCooldownSeconds);
        if (allowedAt.isAfter(LocalDateTime.now())) {
            long retryAfterSeconds = Math.max(1, secondsUntil(allowedAt));
            throw new BusinessConflictException(
                    "Une evaluation vient deja d'etre calculee. Reessayez dans " + retryAfterSeconds + " seconde(s)."
            );
        }
    }

    private void ensureCanCalculate(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if ((current.getRole() == Role.ROLE_ENSEIGNANT || current.getRole() == Role.ROLE_CHEF_EQUIPE)
                && projet.getEncadrantId().equals(current.getId())) {
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

    private ProjetCatalogue getProjetCatalogueAndValidateValide(Long projetId) {
        ProjetCatalogue projet = projetCatalogueRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PROJET_INTROUVABLE + projetId));
        if (projet.getStatut() != StatutProjet.VALIDE) {
            throw new BadRequestException("Le projet doit etre valide pour etre evalue.");
        }
        return projet;
    }

    private boolean isCatalogueEncadrant(User current, ProjetCatalogue projet) {
        return projet.getEncadrant() != null && projet.getEncadrant().getId().equals(current.getId());
    }

    private boolean isCatalogueChefEquipe(User current, ProjetCatalogue projet) {
        return projet.getEquipeRecherche() != null
                && projet.getEquipeRecherche().getChef() != null
                && projet.getEquipeRecherche().getChef().getId().equals(current.getId());
    }

    private void ensureCanCalculateCatalogue(User current, ProjetCatalogue projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if ((current.getRole() == Role.ROLE_ENSEIGNANT || current.getRole() == Role.ROLE_CHEF_EQUIPE)
                && (isCatalogueEncadrant(current, projet) || isCatalogueChefEquipe(current, projet))) {
            return;
        }
        throw new AccessDeniedException("Vous n'etes pas autorise a recalculer le score de ce projet.");
    }

    private void ensureCanViewCatalogue(User current, ProjetCatalogue projet) {
        // Projet publié : visible comme le détail catalogue (tout utilisateur authentifié).
        if (StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())) {
            return;
        }
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        if ((current.getRole() == Role.ROLE_ENSEIGNANT || current.getRole() == Role.ROLE_CHEF_EQUIPE)
                && (isCatalogueEncadrant(current, projet) || isCatalogueChefEquipe(current, projet))) {
            return;
        }
        throw new AccessDeniedException("Vous n'etes pas autorise a consulter cette evaluation.");
    }

    private void ensureNoDuplicateEvaluationCatalogue(Long projetId) {
        if (recalculationCooldownSeconds <= 0) {
            return;
        }
        Evaluation latest = evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(projetId).orElse(null);
        if (latest == null || latest.getDateCalcul() == null) {
            return;
        }
        LocalDateTime allowedAt = latest.getDateCalcul().plusSeconds(recalculationCooldownSeconds);
        if (allowedAt.isAfter(LocalDateTime.now())) {
            long retryAfterSeconds = Math.max(1, secondsUntil(allowedAt));
            throw new BusinessConflictException(
                    "Une evaluation vient deja d'etre calculee. Reessayez dans " + retryAfterSeconds + " seconde(s)."
            );
        }
    }

    private void ensureCanValidate(User current) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        throw new AccessDeniedException("Vous n'etes pas autorise a valider cette evaluation.");
    }

    private long secondsUntil(LocalDateTime allowedAt) {
        return java.time.Duration.between(
                java.time.Instant.now(),
                allowedAt.atZone(java.time.ZoneId.systemDefault()).toInstant()
        ).toSeconds();
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
                        resultat.getCriterionStatus(),
                        resultat.getEvidenceQuality(),
                        resultat.getExtractionQuality(),
                        resultat.getSourceCoverage(),
                        resultat.getEliminatoryState(),
                        splitLines(resultat.getAnalysisMethods()),
                        resultat.getEvidenceJson(),
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
        int eliminatoiresNonSatisfaits = (int) resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> r.reponseEliminatoire() == ReponseEliminatoire.NOT_OK)
                .count();
        int eliminatoiresIndetermines = (int) resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> r.reponseEliminatoire() == null
                        || STATUT_INDETERMINATE.equals(safeUpper(r.eliminatoryState())))
                .count();
        int eliminatoiresConfigurationIncomplete = (int) resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(this::isConfigurationIncomplete)
                .count();
        int notesNonConfigures = totalNotes - notesConfigures;
        List<CriterionConfigurationIssueResponse> incompleteCriteria = resultats.stream()
                .filter(this::isConfigurationIncomplete)
                .map(this::configurationIssue)
                .toList();
        boolean evaluationComplete = evaluation.getScoreFinal() != null && incompleteCriteria.isEmpty()
                || resultats.stream().allMatch(r -> Boolean.TRUE.equals(r.ruleConfigured()));
        List<String> warningCriteriaNames = resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> r.reponseEliminatoire() == ReponseEliminatoire.NOT_OK)
                .map(ResultatCritereResponse::critereLibelle)
                .toList();
        String coverageMessage = coverageMessage(evaluation, totalNotes, notesConfigures, incompleteCriteria.size());

        Long sujetProjetId = evaluation.getSujetProjet() == null ? null : evaluation.getSujetProjet().getId();
        Long projetCatalogueId = evaluation.getProjetCatalogue() == null ? null : evaluation.getProjetCatalogue().getId();
        String evaluationContext = contexteEvaluation(sujetProjetId, projetCatalogueId);

        return new EvaluationResponse(
                evaluation.getId(),
                sujetProjetId,
                projetCatalogueId,
                evaluationContext,
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
                eliminatoiresNonSatisfaits,
                eliminatoiresIndetermines,
                eliminatoiresConfigurationIncomplete,
                incompleteCriteria.size(),
                incompleteCriteria,
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
                evaluation.getErrorMessage(),
                evaluation.getTrigger(),
                evaluation.getCorrelationId(),
                evaluation.getEngineType(),
                modelDisplayName(evaluation.getModelName()),
                evaluation.getPipelineVersion(),
                evaluation.getCriteriaVersion(),
                evaluation.getProjectSnapshotHash(),
                evaluation.getDeliverableSnapshotHash(),
                evaluation.getInputSnapshotHash(),
                evaluation.getProcessingStatus(),
                evaluation.getEligibilityStatus()
        );
    }

    private boolean isConfigurationIncomplete(ResultatCritereResponse resultat) {
        String status = safeUpper(resultat.criterionStatus());

        if ("SCORED".equals(status) || "OK".equals(status)) {
            return false;
        }

        if (STATUT_CONFIGURATION_INCOMPLETE.equals(status)) {
            return true;
        }

        return safeList(resultat.weaknesses()).stream()
                .map(this::safeUpper)
                .anyMatch(value -> Set.of(
                        "MISSING_CRITERION_NAME",
                        "MISSING_CRITERION_DESCRIPTION",
                        "MISSING_MAXIMUM_SCORE",
                        "MISSING_REQUIRED_EVIDENCE_TYPES",
                        "MISSING_CRITERION_FAMILY",
                        "MISSING_POSITIVE_EVIDENCE_EXPECTATIONS",
                        "MISSING_NEGATIVE_EVIDENCE_EXPECTATIONS",
                        "MISSING_PERMITTED_EVIDENCE_NATURES",
                        "MISSING_MINIMUM_SUPPORTING_EVIDENCE_COUNT",
                        "MISSING_SCORING_POLICY",
                        "MISSING_ELIMINATORY_DECISION_RULE"
                ).contains(value));
    }

    private CriterionConfigurationIssueResponse configurationIssue(ResultatCritereResponse resultat) {
        List<String> missingFields = safeList(resultat.weaknesses()).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(value -> value.startsWith("MISSING_"))
                .map(this::configurationFieldFromReason)
                .distinct()
                .toList();
        if (missingFields.isEmpty()) {
            missingFields = List.of("configuration");
        }
        return new CriterionConfigurationIssueResponse(
                resultat.critereId(),
                resultat.critereLibelle(),
                resultat.typeCritere() == null ? null : resultat.typeCritere().name(),
                missingFields,
                List.of(),
                evaluationConfigurationVersion(resultat),
                "Non evalue"
        );
    }

    private String configurationFieldFromReason(String reason) {
        return switch (safeUpper(reason)) {
            case "MISSING_CRITERION_NAME" -> "name";
            case "MISSING_CRITERION_DESCRIPTION" -> "description";
            case "MISSING_MAXIMUM_SCORE" -> "maximumScore";
            case "MISSING_REQUIRED_EVIDENCE_TYPES" -> "requiredEvidenceTypes";
            case "MISSING_CRITERION_FAMILY" -> "criterionFamily";
            case "MISSING_POSITIVE_EVIDENCE_EXPECTATIONS" -> "positiveEvidenceExpectations";
            case "MISSING_NEGATIVE_EVIDENCE_EXPECTATIONS" -> "negativeEvidenceExpectations";
            case "MISSING_CRITERION_VERSION" -> "version";
            case "MISSING_APPLICABILITY" -> "applicability";
            case "MISSING_PERMITTED_EVIDENCE_NATURES" -> "permittedEvidenceNatures";
            case "MISSING_MINIMUM_SUPPORTING_EVIDENCE_COUNT" -> "minimumSupportingEvidenceCount";
            case "MISSING_SCORING_POLICY" -> "scoringPolicy";
            case "MISSING_ELIMINATORY_DECISION_RULE" -> "eliminatoryDecisionRule";
            default -> reason;
        };
    }

    private String evaluationConfigurationVersion(ResultatCritereResponse resultat) {
        return safeList(resultat.weaknesses()).stream()
                .filter(value -> value.startsWith("CONFIG_VERSION:"))
                .map(value -> value.substring("CONFIG_VERSION:".length()))
                .findFirst()
                .orElse(criteriaVersion);
    }

    private String coverageMessage(Evaluation evaluation,
                                   int totalNotes,
                                   int configuredNotes,
                                   int incompleteCriteriaCount) {
        if (evaluation.getScoreFinal() != null) {
            String status = safeUpper(evaluation.getProcessingStatus());

            if ("COMPLETED_WITH_WARNINGS".equals(status)) {
                return "Un score provisoire a été calculé à partir des preuves validées. Une revue reste requise pour les éléments indéterminés.";
            }

            return "Le score a été calculé à partir des critères actifs et des preuves validées. "
                    + configuredNotes + "/" + totalNotes + " critère(s) noté(s) ont contribué.";
        }

        if (incompleteCriteriaCount > 0) {
            return "Les critères actifs nécessitent une configuration complète avant l’évaluation.";
        }

        return "Aucun score n’a été calculé.";
    }

    private String modelDisplayName(String modelName) {
        if (!StringUtils.hasText(modelName)) {
            return modelName;
        }
        String normalized = modelName.replace("\\", "/");
        if (normalized.contains("/")) {
            return normalized.substring(normalized.lastIndexOf('/') + 1);
        }
        return normalized;
    }

    /** Synthese des champs issus du moteur ML, ou valeurs neutres en mode manuel. */
    private record MlSynthese(String statut, String engineType, String modelName, String modelVersion,
                              String pipelineVersion, String criteriaVersion, String effectiveStatus,
                              Double globalConfidence, List<String> warnings, List<String> strengths,
                              List<String> weaknesses, List<String> recommendations) {
    }

    /** Champs projet exploites par le moteur ML, communs aux sujets et aux projets catalogue. */
    private record ProjetContexte(Long id, String titre, String domaine,
                                  String technologies, String categorieName) {
    }

    /** Criteres actifs au moment du calcul, avec la note maximale active associee. */
    private record CriteresActifs(List<CritereEliminatoire> eliminatoires,
                                  List<CritereNote> notes,
                                  int maxNoteValue) {
    }

    /** Empreintes des entrees, tracees pour rendre une evaluation reproductible. */
    private record SnapshotHashes(String projet, String livrables, String input) {
    }

    /** Agregat pondere intermediaire du calcul de score. */
    private record ScoreAgregat(double sommeScores, double sommePoids) {
    }

    /** Tracabilite du calcul d'une note : regle appliquee, metadonnees et explication. */
    private record TraceCalcul(String matchedRule, String metadataUsed, String explanation) {
    }

    private record EvaluationData(
            List<ResultatCritere> resultats,
            Double scoreFinal,
            Boolean eligibleIndustrialisation,
            Boolean hasEliminatoryWarnings,
            List<String> eliminatoryWarnings,
            String commentaire,
            String mlStatus,
            String trigger,
            String correlationId,
            String engineType,
            String modelName,
            String mlModelVersion,
            String pipelineVersion,
            String criteriaVersion,
            String projectSnapshotHash,
            String deliverableSnapshotHash,
            String inputSnapshotHash,
            String processingStatus,
            String eligibilityStatus,
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
