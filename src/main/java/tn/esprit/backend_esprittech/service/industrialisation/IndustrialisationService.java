package tn.esprit.backend_esprittech.service.industrialisation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.industrialisation.*;
import tn.esprit.backend_esprittech.dto.livrable.CataloguePublicLivrableResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.ResultatCritereResponse;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.industrialisation.*;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.EliminatoryWarningsConfirmationRequiredException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;
import tn.esprit.backend_esprittech.service.historique.HistoriqueService;
import tn.esprit.backend_esprittech.service.livrable.LivrableCatalogueService;
import tn.esprit.backend_esprittech.service.livrable.LivrableService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IndustrialisationService {

    private static final String MISSING_LIVRABLE_WARNING =
            "Aucun livrable nest déposé pour ce projet. La CI verra cette alerte.";

    private static final Set<StatutIndustrialisation> OPEN_STATUSES = Set.of(
            StatutIndustrialisation.BROUILLON,
            StatutIndustrialisation.SOUMISE,
            StatutIndustrialisation.RECUE_PAR_CI,
            StatutIndustrialisation.A_COMPLETER,
            StatutIndustrialisation.RECEVABLE
    );

    private static final Set<StatutIndustrialisation> CI_VISIBLE_STATUSES = Set.of(
            StatutIndustrialisation.SOUMISE,
            StatutIndustrialisation.RECUE_PAR_CI,
            StatutIndustrialisation.A_COMPLETER,
            StatutIndustrialisation.RECEVABLE,
            StatutIndustrialisation.GO,
            StatutIndustrialisation.NO_GO,
            StatutIndustrialisation.REFUSEE
    );

    private final CandidatureIndustrialisationRepository candidatureRepository;
    private final QuestionIndustrialisationRepository questionRepository;
    private final ReponseIndustrialisationRepository reponseRepository;
    private final HistoriqueService historiqueService;
    private final SujetProjetRepository sujetProjetRepository;
    private final UserService userService;
    private final EvaluationService evaluationService;
    private final LivrableService livrableService;
    private final MinioStorageService storageService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProjetCatalogueRepository projetCatalogueRepository;
    private final LivrableCatalogueService livrableCatalogueService;

    @Historise(action = ActionType.CREATE, entityType = EntityType.CANDIDATURE_INDUSTRIALISATION, entityIdExpression = "#result.id()", newValuesExpression = "{'statut': #result.statut().name(), 'typeIndustrialisation': #result.typeIndustrialisation().name()}", metadataExpression = "{'typeIndustrialisation': #request.typeIndustrialisation().name()}", summaryExpression = "'Création demande industrialisation projet ' + @industrialisationService.getProjetTitreById(#projetId)")
    public CandidatureIndustrialisationResponse create(Long projetId, CandidatureIndustrialisationRequest request) {
        User current = userService.getCurrentUser();
        SujetProjet projet = getProjet(projetId);
        ensureOwner(current, projet);
        if (projet.getStatut() != StatutSujet.REALISATION_TERMINEE) {
            throw new BusinessException("Le projet doit etre termine avant une demande d'industrialisation.");
        }
        List<CandidatureIndustrialisation> existingRequests = candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(projetId);
        for (CandidatureIndustrialisation existing : existingRequests) {
            if (!OPEN_STATUSES.contains(existing.getStatut())) {
                continue;
            }
            if (existing.getStatut() == StatutIndustrialisation.BROUILLON
                    || existing.getStatut() == StatutIndustrialisation.A_COMPLETER) {
                return toResponse(existing, true);
            }
            throw new BusinessException("Une demande d'industrialisation est deja en cours pour ce projet.");
        }
        List<BlocageEliminatoireDto> warnings = collectProjectEvaluationWarnings(projet);
        requireConfirmationIfNeeded(warnings, request.isConfirmed());

        CandidatureIndustrialisation candidature = new CandidatureIndustrialisation();
        candidature.setSujetProjet(projet);
        candidature.setTypeIndustrialisation(request.typeIndustrialisation());
        candidature.setDemandeur(current);
        candidature.setCommentaire(trimToNull(request.commentaire()));
        candidature.setScoreEvaluationProjet(projet.getScoreFinalEvaluation());
        applyWarnings(candidature, warnings);

        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);
        return toResponse(saved, true);
    }

    /**
     * Demande d'industrialisation directement sur un projet du catalogue applicatif (ProjetCatalogue),
     * qu'il provienne d'un sujet terminé ou d'un dépôt manuel. Contrairement au pipeline SujetProjet,
     * il n'y a pas d'évaluation ML ici : l'éligibilité repose uniquement sur le statut VALIDE du projet.
     */
    @Historise(action = ActionType.CREATE, entityType = EntityType.CANDIDATURE_INDUSTRIALISATION, entityIdExpression = "#result.id()", newValuesExpression = "{'statut': #result.statut().name(), 'typeIndustrialisation': #result.typeIndustrialisation().name()}", metadataExpression = "{'typeIndustrialisation': #request.typeIndustrialisation().name()}", summaryExpression = "'Création demande industrialisation projet ' + @industrialisationService.getProjetCatalogueTitreById(#projetId)")
    public CandidatureIndustrialisationResponse createForCatalogue(Long projetId, CandidatureIndustrialisationRequest request) {
        User current = userService.getCurrentUser();
        ProjetCatalogue projet = getProjetCatalogue(projetId);
        ensureOwnerCatalogue(current, projet);
        if (projet.getStatut() != StatutProjet.VALIDE) {
            throw new BusinessException("Le projet doit etre valide avant une demande d'industrialisation.");
        }
        List<CandidatureIndustrialisation> existingRequests = candidatureRepository.findByProjetCatalogueIdOrderByDateDemandeDesc(projetId);
        for (CandidatureIndustrialisation existing : existingRequests) {
            if (!OPEN_STATUSES.contains(existing.getStatut())) {
                continue;
            }
            if (existing.getStatut() == StatutIndustrialisation.BROUILLON
                    || existing.getStatut() == StatutIndustrialisation.A_COMPLETER) {
                return toResponse(existing, true);
            }
            throw new BusinessException("Une demande d'industrialisation est deja en cours pour ce projet.");
        }

        List<BlocageEliminatoireDto> warnings = collectCatalogueEvaluationWarnings(projet);
        requireConfirmationIfNeeded(warnings, request.isConfirmed());

        CandidatureIndustrialisation candidature = new CandidatureIndustrialisation();
        candidature.setProjetCatalogue(projet);
        // Traceability: ProjetCatalogue.sujetId keeps the origin SujetProjet when conversion created the catalogue entry.
        candidature.setTypeIndustrialisation(request.typeIndustrialisation());
        candidature.setDemandeur(current);
        candidature.setCommentaire(trimToNull(request.commentaire()));
        candidature.setScoreEvaluationProjet(resolveCatalogueScoreSnapshot(projet));
        applyWarnings(candidature, warnings);

        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);
        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public IndustrialisationFormResponse getFormulaire(Long candidatureId) {
        CandidatureIndustrialisation candidature = getCandidature(candidatureId);
        ensureCanRead(userService.getCurrentUser(), candidature);
        List<QuestionIndustrialisationResponse> questions = questionRepository.findByActifTrueOrderByOrdreAsc()
                .stream()
                .map(this::toQuestionResponse)
                .toList();
        List<ReponseIndustrialisationResponse> reponses = reponseRepository.findByCandidatureIndustrialisationId(candidatureId)
                .stream()
                .map(this::toReponseResponse)
                .toList();
        return new IndustrialisationFormResponse(toResponse(candidature, true), questions, reponses);
    }

    public CandidatureIndustrialisationResponse saveReponses(Long candidatureId, ReponsesIndustrialisationRequest request) {
        CandidatureIndustrialisation candidature = getCandidature(candidatureId);
        User current = userService.getCurrentUser();
        ensureOwnerOfCandidature(current, candidature);
        ensureEditable(candidature);

        for (ReponseIndustrialisationRequest reponseRequest : request.reponses()) {
            QuestionIndustrialisation question = questionRepository.findById(reponseRequest.questionId())
                    .filter(q -> Boolean.TRUE.equals(q.getActif()))
                    .orElseThrow(() -> new ResourceNotFoundException("Question industrialisation introuvable: " + reponseRequest.questionId()));
            ReponseIndustrialisation reponse = reponseRepository
                    .findByCandidatureIndustrialisationIdAndQuestionId(candidatureId, question.getId())
                    .orElseGet(() -> {
                        ReponseIndustrialisation created = new ReponseIndustrialisation();
                        created.setCandidatureIndustrialisation(candidature);
                        created.setQuestion(question);
                        return created;
                    });
            applyReponse(reponse, reponseRequest, question);
            reponseRepository.save(reponse);
        }

        return toResponse(candidatureRepository.save(candidature), true);
    }

    public CandidatureIndustrialisationResponse uploadPreuve(Long candidatureId, Long questionId, MultipartFile file) {
        CandidatureIndustrialisation candidature = getCandidature(candidatureId);
        User current = userService.getCurrentUser();
        ensureOwnerOfCandidature(current, candidature);
        ensureEditable(candidature);
        QuestionIndustrialisation question = questionRepository.findById(questionId)
                .filter(q -> Boolean.TRUE.equals(q.getActif()))
                .orElseThrow(() -> new ResourceNotFoundException("Question industrialisation introuvable: " + questionId));
        livrableService.validateFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String objectName = "industrialisation-preuves/" + candidatureId + "/" + questionId + "/"
                + UUID.randomUUID() + "-" + originalFileName.replaceAll("[^A-Za-z0-9._-]", "_");
        MinioStorageService.StoredObject storedObject = storageService.upload(objectName, file);

        ReponseIndustrialisation reponse = reponseRepository
                .findByCandidatureIndustrialisationIdAndQuestionId(candidatureId, questionId)
                .orElseGet(() -> {
                    ReponseIndustrialisation created = new ReponseIndustrialisation();
                    created.setCandidatureIndustrialisation(candidature);
                    created.setQuestion(question);
                    return created;
                });
        reponse.setPreuveObjectName(storedObject.objectName());
        reponse.setPreuveOriginalFileName(originalFileName);
        reponse.setPreuveContentType(storedObject.contentType());
        reponse.setPreuveSize(storedObject.size());
        reponseRepository.save(reponse);

        return toResponse(candidatureRepository.save(candidature), true);
    }

    public CandidatureIndustrialisationResponse soumettre(Long candidatureId) {
        return soumettre(candidatureId, null);
    }

    @Historise(action = ActionType.SUBMIT, entityType = EntityType.CANDIDATURE_INDUSTRIALISATION, entityIdExpression = "#candidatureId", oldValuesExpression = "@industrialisationService.getOldValues(#candidatureId)", newValuesExpression = "{'statut': 'SOUMISE'}", summaryExpression = "'Soumission demande industrialisation projet ' + @industrialisationService.getProjetTitreByCandidatureId(#candidatureId)")
    public CandidatureIndustrialisationResponse soumettre(Long candidatureId, SubmitIndustrialisationRequest request) {
        CandidatureIndustrialisation candidature = getCandidature(candidatureId);
        User current = userService.getCurrentUser();
        ensureOwnerOfCandidature(current, candidature);
        ensureEditable(candidature);

        boolean hasActiveLivrables;
        String projetTitre;
        if (candidature.getSujetProjet() != null) {
            SujetProjet projet = candidature.getSujetProjet();
            projetTitre = projet.getTitre();
            if (projet.getScoreFinalEvaluation() == null) {
                throw new BusinessException("Le projet doit etre evalue avant soumission a la CI.");
            }
            hasActiveLivrables = hasEffectiveLivrables(candidature);

            validateCompleteness(candidature);
            List<BlocageEliminatoireDto> warnings = collectEliminatoryWarnings(candidature);
            requireConfirmationIfNeeded(warnings, request != null && request.isConfirmed());
            applyWarnings(candidature, warnings);

            candidature.setStatut(StatutIndustrialisation.SOUMISE);
            candidature.setDateSoumission(java.time.LocalDateTime.now());
            candidature.setDateReceptionCI(java.time.LocalDateTime.now());
            projet.setStatut(candidature.getTypeIndustrialisation() == TypeIndustrialisation.INTERNE
                    ? StatutSujet.CANDIDAT_INDUSTRIALISATION_INTERNE
                    : StatutSujet.CANDIDAT_INDUSTRIALISATION_EXTERNE);
            sujetProjetRepository.save(projet);
        } else {
            ProjetCatalogue projet = candidature.getProjetCatalogue();
            projetTitre = projet.getTitre();
            hasActiveLivrables = hasEffectiveLivrables(candidature);

            validateCompleteness(candidature);
            List<BlocageEliminatoireDto> warnings = collectEliminatoryWarnings(candidature);
            requireConfirmationIfNeeded(warnings, request != null && request.isConfirmed());
            applyWarnings(candidature, warnings);

            candidature.setStatut(StatutIndustrialisation.SOUMISE);
            candidature.setDateSoumission(java.time.LocalDateTime.now());
            candidature.setDateReceptionCI(java.time.LocalDateTime.now());
            projet.setStatut(candidature.getTypeIndustrialisation() == TypeIndustrialisation.INTERNE
                    ? StatutProjet.CANDIDAT_INDUSTRIALISATION_INTERNE
                    : StatutProjet.CANDIDAT_INDUSTRIALISATION_EXTERNE);
            projetCatalogueRepository.save(projet);
        }
        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);

        List<User> ciMembers = userRepository.findByRole(Role.ROLE_CI);
        for (User ciMember : ciMembers) {
            notificationService.creerNotification(
                    ciMember.getId(),
                    NotificationType.CI_DEMANDE_SOUMISE,
                    "Demande d'industrialisation",
                    "Nouvelle demande d'industrialisation soumise pour le projet \"" + projetTitre + "\".",
                    "/ci/industrialisation");
        }

        return toResponse(saved, true, !hasActiveLivrables);
    }

    @Transactional(readOnly = true)
    public List<CandidatureIndustrialisationResponse> mesDemandes() {
        User current = userService.getCurrentUser();
        return candidatureRepository.findByDemandeurIdOrderByDateDemandeDesc(current.getId())
                .stream()
                .map(c -> toResponse(c, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidatureIndustrialisationResponse> listForCi(StatutIndustrialisation statut, TypeIndustrialisation type) {
        ensureCi(userService.getCurrentUser());
        List<CandidatureIndustrialisation> candidatures = statut == null
                ? candidatureRepository.findByStatutInOrderByDateDemandeDesc(CI_VISIBLE_STATUSES)
                : candidatureRepository.findByStatutInOrderByDateDemandeDesc(List.of(statut));
        return candidatures.stream()
                .filter(c -> type == null || c.getTypeIndustrialisation() == type)
                .map(c -> toResponse(c, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CandidatureIndustrialisationResponse> listForAdmin() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Acces admin requis");
        }
        return candidatureRepository.findAllByOrderByDateDemandeDesc()
                .stream()
                .map(c -> toResponse(c, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public CandidatureIndustrialisationResponse detailForCi(Long id) {
        ensureCi(userService.getCurrentUser());
        return toResponse(getCandidature(id), true);
    }

    @Transactional(readOnly = true)
    public CandidatureIndustrialisationResponse detail(Long id) {
        CandidatureIndustrialisation candidature = getCandidature(id);
        ensureCanRead(userService.getCurrentUser(), candidature);
        return toResponse(candidature, true);
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

    public String getProjetTitreByCandidatureId(Long candidatureId) {
        return candidatureRepository.findById(candidatureId)
                .map(c -> c.getSujetProjet() != null ? c.getSujetProjet().getTitre() : c.getProjetCatalogue().getTitre())
                .orElse("Inconnu");
    }

    public Map<String, Object> getOldValues(Long id) {
        CandidatureIndustrialisation c = getCandidature(id);
        Map<String, Object> map = new HashMap<>();
        map.put("statut", c.getStatut().name());
        return map;
    }

    @Historise(action = ActionType.DECISION_GO, entityType = EntityType.CANDIDATURE_INDUSTRIALISATION, entityIdExpression = "#id", oldValuesExpression = "@industrialisationService.getOldValues(#id)", newValuesExpression = "{'statut': 'GO', 'orientation': #request.orientation().name()}", metadataExpression = "{'commentaire': #request.commentaire()}", summaryExpression = "'Décision GO - ' + #request.orientation().name()")
    public CandidatureIndustrialisationResponse decideGo(Long id, DecisionGoRequest request) {
        User current = userService.getCurrentUser();
        ensureCi(current);
        CandidatureIndustrialisation candidature = getCandidature(id);
        ensureDecisionAllowed(candidature);
        if (request.orientation() == null) {
            throw new BusinessException("L'orientation est obligatoire pour une decision Go.");
        }

        candidature.setStatut(StatutIndustrialisation.GO);
        candidature.setDecisionGoNoGo(true);
        candidature.setOrientation(request.orientation());
        candidature.setMotifDecision(trimToNull(request.commentaire()));
        candidature.setCiDecideur(current);
        candidature.setDateDecisionCI(java.time.LocalDateTime.now());

        String projetTitre;
        if (candidature.getSujetProjet() != null) {
            SujetProjet projet = candidature.getSujetProjet();
            projetTitre = projet.getTitre();
            projet.setStatut(request.orientation() == OrientationIndustrialisation.DSI
                    ? StatutSujet.INDUSTRIALISE_DSI
                    : StatutSujet.INDUSTRIALISE_EXTERNE);
            sujetProjetRepository.save(projet);
        } else {
            ProjetCatalogue projet = candidature.getProjetCatalogue();
            projetTitre = projet.getTitre();
            projet.setStatut(request.orientation() == OrientationIndustrialisation.DSI
                    ? StatutProjet.INDUSTRIALISE_DSI
                    : StatutProjet.INDUSTRIALISE_EXTERNE);
            projetCatalogueRepository.save(projet);
        }

        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);

        notificationService.creerNotification(
                candidature.getDemandeur().getId(),
                NotificationType.CI_DEMANDE_APPOUVEE,
                "Demande industrialisation approuvee",
                "Votre demande d'industrialisation pour le projet \"" + projetTitre + "\" a ete approuvee (GO - " + request.orientation() + ").",
                "/frontoffice/demandes-industrialisation");

        return toResponse(saved, true);
    }

    @Historise(action = ActionType.DECISION_NOGO, entityType = EntityType.CANDIDATURE_INDUSTRIALISATION, entityIdExpression = "#id", oldValuesExpression = "@industrialisationService.getOldValues(#id)", newValuesExpression = "{'statut': 'NO_GO'}", metadataExpression = "{'motif': #request.motif()}", summaryExpression = "'Décision NO GO projet ' + @industrialisationService.getProjetTitreByCandidatureId(#id)")
    public CandidatureIndustrialisationResponse decideNoGo(Long id, DecisionNoGoRequest request) {
        User current = userService.getCurrentUser();
        ensureCi(current);
        CandidatureIndustrialisation candidature = getCandidature(id);
        ensureDecisionAllowed(candidature);
        if (!StringUtils.hasText(request.motif())) {
            throw new BusinessException("Le motif est obligatoire pour une decision No Go.");
        }

        candidature.setStatut(StatutIndustrialisation.NO_GO);
        candidature.setDecisionGoNoGo(false);
        candidature.setMotifDecision(request.motif().trim());
        candidature.setCiDecideur(current);
        candidature.setDateDecisionCI(java.time.LocalDateTime.now());
        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);

        String projetTitre = candidature.getSujetProjet() != null
                ? candidature.getSujetProjet().getTitre()
                : candidature.getProjetCatalogue().getTitre();
        notificationService.creerNotification(
                candidature.getDemandeur().getId(),
                NotificationType.CI_DEMANDE_REJETEE,
                "Demande industrialisation refusee",
                "Votre demande d'industrialisation pour le projet \"" + projetTitre + "\" a ete refusee. Motif : " + request.motif(),
                "/frontoffice/demandes-industrialisation");

        return toResponse(saved, true);
    }

    private void validateCompleteness(CandidatureIndustrialisation candidature) {
        List<QuestionIndustrialisation> questions = questionRepository.findByActifTrueOrderByOrdreAsc();
        Map<Long, ReponseIndustrialisation> reponsesByQuestion = reponseRepository
                .findByCandidatureIndustrialisationId(candidature.getId())
                .stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), Function.identity()));

        for (QuestionIndustrialisation question : questions) {
            if (!Boolean.TRUE.equals(question.getObligatoire())) {
                continue;
            }
            ReponseIndustrialisation reponse = reponsesByQuestion.get(question.getId());
            String missingReason = missingAnswerReason(question, reponse);
            if (missingReason != null) {
                throw new BusinessException(missingReason);
            }
        }
    }

    private List<BlocageEliminatoireDto> collectEliminatoryWarnings(CandidatureIndustrialisation candidature) {
        List<BlocageEliminatoireDto> warnings = new java.util.ArrayList<>();
        if (candidature.getSujetProjet() != null) {
            warnings.addAll(collectProjectEvaluationWarnings(candidature.getSujetProjet()));
        } else if (candidature.getProjetCatalogue() != null) {
            warnings.addAll(collectCatalogueEvaluationWarnings(candidature.getProjetCatalogue()));
        }
        Map<Long, ReponseIndustrialisation> reponsesByQuestion = reponseRepository
                .findByCandidatureIndustrialisationId(candidature.getId())
                .stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), Function.identity()));
        for (QuestionIndustrialisation question : questionRepository.findByActifTrueOrderByOrdreAsc()) {
            if (question.getTypeCritere() != TypeCritere.ELIMINATOIRE) {
                continue;
            }
            ReponseIndustrialisation reponse = reponsesByQuestion.get(question.getId());
            ReponseEliminatoire result = IndustrialisationQuestionEvaluator.eliminatoryResult(question, reponse);
            if (result == ReponseEliminatoire.NOT_OK) {
                warnings.add(new BlocageEliminatoireDto(
                        "QUESTIONNAIRE",
                        question.getId(),
                        question.getLibelle(),
                        "Reponse non conforme pour une question eliminatoire."
                ));
            }
        }
        return warnings;
    }

    private List<BlocageEliminatoireDto> collectProjectEvaluationWarnings(SujetProjet projet) {
        EvaluationResponse latestEvaluation = findLatestEvaluation(projet.getId());
        if (latestEvaluation == null || latestEvaluation.resultats() == null) {
            return List.of();
        }
        return latestEvaluation.resultats().stream()
                .filter(resultat -> resultat.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(resultat -> resultat.reponseEliminatoire() == ReponseEliminatoire.NOT_OK)
                .map(this::toWarning)
                .toList();
    }

    private BlocageEliminatoireDto toWarning(ResultatCritereResponse resultat) {
        return new BlocageEliminatoireDto(
                "EVALUATION_PROJET",
                resultat.critereId(),
                resultat.critereLibelle(),
                StringUtils.hasText(resultat.explanation())
                        ? resultat.explanation()
                        : "Critere eliminatoire projet NOT_OK."
        );
    }

    private void requireConfirmationIfNeeded(List<BlocageEliminatoireDto> warnings, boolean confirmed) {
        if (!warnings.isEmpty() && !confirmed) {
            throw new EliminatoryWarningsConfirmationRequiredException(warnings.size(), warnings);
        }
    }

    private void applyWarnings(CandidatureIndustrialisation candidature, List<BlocageEliminatoireDto> warnings) {
        candidature.setHasEliminatoryWarnings(!warnings.isEmpty());
        candidature.setEliminatoryWarningsCount(warnings.size());
        candidature.setEliminatoryWarningsDetails(warnings.stream()
                .map(warning -> warning.source() + "|" + warning.referenceId() + "|" + warning.libelle() + "|" + warning.raison())
                .collect(Collectors.joining("\n")));
    }

    private String missingAnswerReason(QuestionIndustrialisation question, ReponseIndustrialisation reponse) {
        if (reponse == null) {
            return "Reponse obligatoire manquante: " + question.getLibelle();
        }
        String typeError = switch (question.getTypeReponse()) {
            case TEXTE, CHOIX -> StringUtils.hasText(reponse.getValeurTexte())
                    ? null
                    : "Reponse texte manquante pour: " + question.getLibelle();
            case BOOLEAN -> reponse.getValeurBoolean() != null
                    ? null
                    : "Reponse booleenne manquante pour: " + question.getLibelle();
            case NUMERIQUE -> reponse.getValeurNumerique() != null
                    ? null
                    : "Reponse numerique manquante pour: " + question.getLibelle();
            case URL -> StringUtils.hasText(reponse.getValeurUrl())
                    ? null
                    : "URL manquante pour: " + question.getLibelle();
            case FICHIER -> StringUtils.hasText(reponse.getPreuveObjectName())
                    ? null
                    : "Preuve fichier manquante pour: " + question.getLibelle();
        };
        if (typeError != null) {
            return typeError;
        }
        if (question.getTypeCritere() == tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere.ELIMINATOIRE
                && IndustrialisationQuestionEvaluator.eliminatoryResult(question, reponse) == null) {
            return "Reponse eliminatoire OK/NOT_OK manquante pour: " + question.getLibelle();
        }
        return null;
    }

    private void applyReponse(ReponseIndustrialisation reponse,
                              ReponseIndustrialisationRequest request,
                              QuestionIndustrialisation question) {
        reponse.setValeurTexte(trimToNull(request.valeurTexte()));
        reponse.setValeurBoolean(request.valeurBoolean());
        reponse.setValeurNumerique(request.valeurNumerique());
        reponse.setValeurUrl(trimToNull(request.valeurUrl()));
        reponse.setReponseEliminatoire(IndustrialisationQuestionEvaluator.eliminatoryResult(question, reponse));
        reponse.setNoteObtenue(request.noteObtenue());
        reponse.setJustificatif(trimToNull(request.justificatif()));
    }

    private CandidatureIndustrialisation getCandidature(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande industrialisation introuvable avec id: " + id));
    }

    private SujetProjet getProjet(Long id) {
        return sujetProjetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + id));
    }

    private ProjetCatalogue getProjetCatalogue(Long id) {
        return projetCatalogueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + id));
    }

    private void ensureOwner(User current, SujetProjet projet) {
        if (current.getRole() != Role.ROLE_ENSEIGNANT || !projet.getEncadrantId().equals(current.getId())) {
            throw new AccessDeniedException("Seul l'encadrant du projet peut gerer cette demande.");
        }
    }

    private void ensureOwnerCatalogue(User current, ProjetCatalogue projet) {
        if (current.getRole() != Role.ROLE_ENSEIGNANT
                || projet.getEncadrant() == null
                || !projet.getEncadrant().getId().equals(current.getId())) {
            throw new AccessDeniedException("Seul l'encadrant du projet peut gerer cette demande.");
        }
    }

    /** Verifie la propriete de la demande, quelle que soit la source (SujetProjet ou ProjetCatalogue). */
    private void ensureOwnerOfCandidature(User current, CandidatureIndustrialisation candidature) {
        if (candidature.getSujetProjet() != null) {
            ensureOwner(current, candidature.getSujetProjet());
        } else {
            ensureOwnerCatalogue(current, candidature.getProjetCatalogue());
        }
    }

    private void ensureCanRead(User current, CandidatureIndustrialisation candidature) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        if (candidature.getSujetProjet() != null) {
            if (candidature.getSujetProjet().getEncadrantId().equals(current.getId())) {
                return;
            }
        } else if (candidature.getProjetCatalogue() != null
                && candidature.getProjetCatalogue().getEncadrant() != null
                && candidature.getProjetCatalogue().getEncadrant().getId().equals(current.getId())) {
            return;
        }
        throw new AccessDeniedException("Acces refuse a la demande industrialisation");
    }

    private void ensureCi(User current) {
        if (current.getRole() != Role.ROLE_CI) {
            throw new AccessDeniedException("Acces CI requis");
        }
    }

    private void ensureEditable(CandidatureIndustrialisation candidature) {
        if (candidature.getStatut() != StatutIndustrialisation.BROUILLON
                && candidature.getStatut() != StatutIndustrialisation.A_COMPLETER) {
            throw new BusinessException("La demande n'est plus editable.");
        }
    }

    private void ensureDecisionAllowed(CandidatureIndustrialisation candidature) {
        if (!Set.of(
                StatutIndustrialisation.SOUMISE,
                StatutIndustrialisation.RECUE_PAR_CI,
                StatutIndustrialisation.RECEVABLE
        ).contains(candidature.getStatut())) {
            throw new BusinessException("La demande n'est pas en attente de decision CI.");
        }
    }

    private CandidatureIndustrialisationResponse toResponse(CandidatureIndustrialisation candidature, boolean includeDetails) {
        return toResponse(candidature, includeDetails, false);
    }

    private CandidatureIndustrialisationResponse toResponse(CandidatureIndustrialisation candidature,
                                                            boolean includeDetails,
                                                            boolean forceMissingLivrablesWarning) {
        SujetProjet sujetProjet = candidature.getSujetProjet();
        ProjetCatalogue projetCatalogue = candidature.getProjetCatalogue();
        User demandeur = candidature.getDemandeur();
        User decideur = candidature.getCiDecideur();
        List<ReponseIndustrialisationResponse> reponses = includeDetails
                ? reponseRepository.findByCandidatureIndustrialisationId(candidature.getId())
                    .stream()
                    .sorted(Comparator.comparing(r -> r.getQuestion().getOrdre()))
                    .map(this::toReponseResponse)
                    .toList()
                : List.of();
        List<HistoriqueIndustrialisationResponse> historique = includeDetails
                ? historiqueService.findByEntity(EntityType.CANDIDATURE_INDUSTRIALISATION, candidature.getId(), org.springframework.data.domain.Pageable.unpaged()).content()
                    .stream()
                    .map(HistoriqueIndustrialisationResponse::from)
                    .toList()
                : List.of();

        Long projetId;
        String projetTitre;
        StatutSujet projetStatut;
        CategorieSujet projetCategorie;
        String projetDomaine;
        String projetDescription;
        String projetObjectifs;
        String projetTechnologies;
        String projetPrerequis;
        java.time.LocalDateTime projetDateCreation;
        java.time.LocalDateTime projetDateSoumission;
        java.time.LocalDateTime projetDateValidation;
        java.time.LocalDateTime projetDateTerminaison;
        Double scoreEvaluationProjet;
        Boolean eligibleIndustrialisation;
        Boolean bloqueParEliminatoire;
        List<LivrableResponse> livrables;
        EvaluationResponse latestEvaluation;

        if (sujetProjet != null) {
            projetId = sujetProjet.getId();
            projetTitre = sujetProjet.getTitre();
            projetStatut = sujetProjet.getStatut();
            projetCategorie = sujetProjet.getCategorie();
            projetDomaine = sujetProjet.getDomaine();
            projetDescription = sujetProjet.getDescription();
            projetObjectifs = sujetProjet.getObjectifs();
            projetTechnologies = sujetProjet.getTechnologies();
            projetPrerequis = sujetProjet.getPrerequis();
            projetDateCreation = sujetProjet.getDateCreation();
            projetDateSoumission = sujetProjet.getDateSoumission();
            projetDateValidation = sujetProjet.getDateValidation();
            projetDateTerminaison = sujetProjet.getDateTerminaison();
            scoreEvaluationProjet = sujetProjet.getScoreFinalEvaluation() != null
                    ? sujetProjet.getScoreFinalEvaluation()
                    : candidature.getScoreEvaluationProjet();
            eligibleIndustrialisation = sujetProjet.getEligibleIndustrialisation();
            bloqueParEliminatoire = sujetProjet.getBloqueParEliminatoire();
            livrables = includeDetails ? findEffectiveLivrables(candidature) : List.of();
            latestEvaluation = includeDetails ? findLatestEvaluation(sujetProjet.getId()) : null;
        } else if (projetCatalogue != null) {
            projetId = projetCatalogue.getId();
            projetTitre = projetCatalogue.getTitre();
            // Les valeurs de StatutProjet/CategorieSujet portent les memes noms que StatutSujet/CategorieSujet :
            // on reutilise ces enums cote reponse pour ne pas dupliquer le DTO pour les deux sources possibles.
            projetStatut = StatutSujet.valueOf(projetCatalogue.getStatut().name());
            projetCategorie = CategorieSujet.valueOf(projetCatalogue.getTypeProjet().name());
            projetDomaine = joinNoms(projetCatalogue.getDomaines().stream().map(d -> d.getNom()));
            projetDescription = projetCatalogue.getDescription();
            projetObjectifs = projetCatalogue.getObjectifs();
            projetTechnologies = joinNoms(projetCatalogue.getTechnologies().stream().map(t -> t.getNom()));
            projetPrerequis = joinNoms(projetCatalogue.getPrerequis().stream().map(p -> p.getNom()));
            projetDateCreation = projetCatalogue.getDateCreation();
            projetDateSoumission = null;
            projetDateValidation = projetCatalogue.getDateValidation();
            projetDateTerminaison = null;
            scoreEvaluationProjet = candidature.getScoreEvaluationProjet() != null
                    ? candidature.getScoreEvaluationProjet()
                    : resolveCatalogueScoreSnapshot(projetCatalogue);
            EvaluationResponse catalogueEvaluation = includeDetails
                    ? findLatestEvaluationForCatalogue(projetCatalogue)
                    : null;
            eligibleIndustrialisation = catalogueEvaluation != null
                    ? catalogueEvaluation.eligibleIndustrialisation()
                    : null;
            bloqueParEliminatoire = catalogueEvaluation != null
                    ? catalogueEvaluation.bloqueParEliminatoire()
                    : null;
            livrables = includeDetails ? findEffectiveLivrables(candidature) : List.of();
            latestEvaluation = catalogueEvaluation;
        } else {
            throw new BusinessException(
                    "La demande d'industrialisation n'est associee ni a un sujet ni a un projet catalogue.");
        }

        List<String> warnings = collectSubmissionWarnings(livrables, includeDetails, forceMissingLivrablesWarning);

        return new CandidatureIndustrialisationResponse(
                candidature.getId(),
                projetId,
                projetTitre,
                projetStatut,
                projetCategorie,
                projetDomaine,
                projetDescription,
                projetObjectifs,
                projetTechnologies,
                projetPrerequis,
                projetDateCreation,
                projetDateSoumission,
                projetDateValidation,
                projetDateTerminaison,
                scoreEvaluationProjet,
                eligibleIndustrialisation,
                bloqueParEliminatoire,
                candidature.getHasEliminatoryWarnings(),
                candidature.getEliminatoryWarningsCount(),
                parseWarnings(candidature.getEliminatoryWarningsDetails()),
                candidature.getTypeIndustrialisation(),
                candidature.getStatut(),
                demandeur.getId(),
                demandeur.getPrenom() + " " + demandeur.getNom(),
                candidature.getDateDemande(),
                candidature.getDateSoumission(),
                candidature.getDateReceptionCI(),
                decideur == null ? null : decideur.getId(),
                decideur == null ? null : decideur.getPrenom() + " " + decideur.getNom(),
                candidature.getDateDecisionCI(),
                candidature.getDecisionGoNoGo(),
                candidature.getOrientation(),
                candidature.getMotifDecision(),
                candidature.getCommentaire(),
                reponses,
                livrables,
                historique,
                latestEvaluation,
                warnings
        );
    }

    private String joinNoms(java.util.stream.Stream<String> noms) {
        return noms.sorted().collect(Collectors.joining(", "));
    }

    private List<LivrableResponse> findEffectiveLivrables(CandidatureIndustrialisation candidature) {
        ProjetCatalogue projetCatalogue = resolveProjetCatalogue(candidature);
        if (projetCatalogue != null && StatutProjet.STATUTS_CATALOGUE.contains(projetCatalogue.getStatut())) {
            return livrableCatalogueService.findPublishedForPublicCatalogue(projetCatalogue.getId())
                    .stream()
                    .map(this::toLivrableResponse)
                    .toList();
        }
        SujetProjet sujetProjet = candidature.getSujetProjet();
        if (sujetProjet != null) {
            return livrableService.findByProjet(sujetProjet.getId());
        }
        return List.of();
    }

    private boolean hasEffectiveLivrables(CandidatureIndustrialisation candidature) {
        ProjetCatalogue projetCatalogue = resolveProjetCatalogue(candidature);
        if (projetCatalogue != null && StatutProjet.STATUTS_CATALOGUE.contains(projetCatalogue.getStatut())) {
            return !livrableCatalogueService.findPublishedForPublicCatalogue(projetCatalogue.getId()).isEmpty();
        }
        SujetProjet sujetProjet = candidature.getSujetProjet();
        return sujetProjet != null && livrableService.hasActiveLivrables(sujetProjet.getId());
    }

    private ProjetCatalogue resolveProjetCatalogue(CandidatureIndustrialisation candidature) {
        if (candidature.getProjetCatalogue() != null) {
            return candidature.getProjetCatalogue();
        }
        SujetProjet sujetProjet = candidature.getSujetProjet();
        if (sujetProjet == null || sujetProjet.getId() == null) {
            return null;
        }
        return projetCatalogueRepository.findBySujetId(sujetProjet.getId()).orElse(null);
    }

    private LivrableResponse toLivrableResponse(CataloguePublicLivrableResponse livrable) {
        return new LivrableResponse(
                livrable.id(),
                null,
                livrable.projetTitre(),
                livrable.typeLivrable(),
                livrable.nom(),
                livrable.description(),
                livrable.originalFileName(),
                livrable.objectName(),
                livrable.contentType(),
                livrable.size(),
                livrable.lienExterne(),
                livrable.deposantId(),
                livrable.deposantNom(),
                livrable.dateDepot(),
                livrable.actif(),
                livrable.projetId(),
                livrable.fromSujet()
        );
    }

    private List<String> collectSubmissionWarnings(List<LivrableResponse> livrables,
                                                   boolean includeDetails,
                                                   boolean forceMissingLivrablesWarning) {
        if ((forceMissingLivrablesWarning || includeDetails) && livrables.isEmpty()) {
            return List.of(MISSING_LIVRABLE_WARNING);
        }
        return List.of();
    }

    private EvaluationResponse findLatestEvaluation(Long projetId) {
        try {
            return evaluationService.getLatestEvaluationByProjet(projetId);
        } catch (tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException
                 | ResourceNotFoundException ex) {
            return null;
        }
    }

    private EvaluationResponse findLatestEvaluationForCatalogue(ProjetCatalogue projet) {
        try {
            return evaluationService.getLatestEvaluationByProjetCatalogue(projet.getId());
        } catch (tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException
                 | ResourceNotFoundException ex) {
            return null;
        }
    }

    private Double resolveCatalogueScoreSnapshot(ProjetCatalogue projet) {
        if (projet.getScore() != null && projet.getScore() > 0) {
            return projet.getScore().doubleValue();
        }
        if (projet.getSujetId() == null) {
            return null;
        }
        return sujetProjetRepository.findById(projet.getSujetId())
                .map(SujetProjet::getScoreFinalEvaluation)
                .orElse(null);
    }

    private List<BlocageEliminatoireDto> collectCatalogueEvaluationWarnings(ProjetCatalogue projet) {
        EvaluationResponse latestEvaluation = findLatestEvaluationForCatalogue(projet);
        if (latestEvaluation == null || latestEvaluation.resultats() == null) {
            return List.of();
        }
        return latestEvaluation.resultats().stream()
                .filter(resultat -> resultat.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(resultat -> resultat.reponseEliminatoire() == ReponseEliminatoire.NOT_OK)
                .map(this::toWarning)
                .toList();
    }

    private List<String> parseWarnings(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("\\n"))
                .map(line -> line.split("\\|", 4))
                .map(parts -> parts.length >= 4 ? parts[2] + " - " + parts[3] : String.join(" ", parts))
                .toList();
    }

    private QuestionIndustrialisationResponse toQuestionResponse(QuestionIndustrialisation question) {
        return new QuestionIndustrialisationResponse(
                question.getId(),
                question.getLibelle(),
                question.getDescription(),
                question.getTypeReponse(),
                question.getObligatoire(),
                question.getTypeCritere(),
                question.getPoids(),
                question.getOrdre(),
                question.getActif(),
                question.getConditionEliminatoire(),
                question.getDateCreation(),
                question.getDateMiseAJour()
        );
    }

    private ReponseIndustrialisationResponse toReponseResponse(ReponseIndustrialisation reponse) {
        return new ReponseIndustrialisationResponse(
                reponse.getId(),
                reponse.getQuestion().getId(),
                reponse.getQuestion().getLibelle(),
                reponse.getValeurTexte(),
                reponse.getValeurBoolean(),
                reponse.getValeurNumerique(),
                reponse.getValeurUrl(),
                reponse.getPreuveObjectName(),
                reponse.getPreuveOriginalFileName(),
                reponse.getPreuveContentType(),
                reponse.getPreuveSize(),
                reponse.getReponseEliminatoire(),
                reponse.getNoteObtenue(),
                reponse.getJustificatif(),
                reponse.getDateReponse()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
