package tn.esprit.backend_esprittech.service.industrialisation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.dto.industrialisation.*;
import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.industrialisation.*;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.HistoriqueIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;
import tn.esprit.backend_esprittech.service.livrable.LivrableService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.util.Comparator;
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
    private final HistoriqueIndustrialisationRepository historiqueRepository;
    private final SujetProjetRepository sujetProjetRepository;
    private final UserService userService;
    private final EvaluationService evaluationService;
    private final LivrableService livrableService;
    private final MinioStorageService storageService;

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

        CandidatureIndustrialisation candidature = new CandidatureIndustrialisation();
        candidature.setSujetProjet(projet);
        candidature.setTypeIndustrialisation(request.typeIndustrialisation());
        candidature.setDemandeur(current);
        candidature.setCommentaire(trimToNull(request.commentaire()));
        candidature.setScoreEvaluationProjet(projet.getScoreFinalEvaluation());

        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);
        addHistory(saved, null, StatutIndustrialisation.BROUILLON, current, null, "Demande creee");
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
        ensureOwner(current, candidature.getSujetProjet());
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
        ensureOwner(current, candidature.getSujetProjet());
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
        CandidatureIndustrialisation candidature = getCandidature(candidatureId);
        User current = userService.getCurrentUser();
        ensureOwner(current, candidature.getSujetProjet());
        ensureEditable(candidature);

        SujetProjet projet = candidature.getSujetProjet();
        if (projet.getScoreFinalEvaluation() == null) {
            throw new BusinessException("Le projet doit etre evalue avant soumission a la CI.");
        }
        if (!Boolean.TRUE.equals(projet.getEligibleIndustrialisation()) || Boolean.TRUE.equals(projet.getBloqueParEliminatoire())) {
            throw new BusinessException("Projet non eligible a l'industrialisation: critere eliminatoire non conforme.");
        }
        if (!livrableService.hasActiveLivrables(projet.getId())) {
            throw new BusinessException("Au moins un livrable projet est requis avant soumission.");
        }

        validateCompleteness(candidature);

        StatutIndustrialisation oldStatus = candidature.getStatut();
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        candidature.setDateSoumission(java.time.LocalDateTime.now());
        candidature.setDateReceptionCI(java.time.LocalDateTime.now());
        projet.setStatut(candidature.getTypeIndustrialisation() == TypeIndustrialisation.INTERNE
                ? StatutSujet.CANDIDAT_INDUSTRIALISATION_INTERNE
                : StatutSujet.CANDIDAT_INDUSTRIALISATION_EXTERNE);
        sujetProjetRepository.save(projet);
        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);
        addHistory(saved, oldStatus, StatutIndustrialisation.SOUMISE, current, null, "Demande soumise a la CI");
        return toResponse(saved, true);
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

    public CandidatureIndustrialisationResponse decideGo(Long id, DecisionGoRequest request) {
        User current = userService.getCurrentUser();
        ensureCi(current);
        CandidatureIndustrialisation candidature = getCandidature(id);
        ensureDecisionAllowed(candidature);
        if (request.orientation() == null) {
            throw new BusinessException("L'orientation est obligatoire pour une decision Go.");
        }

        StatutIndustrialisation oldStatus = candidature.getStatut();
        candidature.setStatut(StatutIndustrialisation.GO);
        candidature.setDecisionGoNoGo(true);
        candidature.setOrientation(request.orientation());
        candidature.setMotifDecision(trimToNull(request.commentaire()));
        candidature.setCiDecideur(current);
        candidature.setDateDecisionCI(java.time.LocalDateTime.now());

        SujetProjet projet = candidature.getSujetProjet();
        projet.setStatut(request.orientation() == OrientationIndustrialisation.DSI
                ? StatutSujet.INDUSTRIALISE_DSI
                : StatutSujet.INDUSTRIALISE_EXTERNE);
        sujetProjetRepository.save(projet);

        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);
        addHistory(saved, oldStatus, StatutIndustrialisation.GO, current, null, request.commentaire());
        return toResponse(saved, true);
    }

    public CandidatureIndustrialisationResponse decideNoGo(Long id, DecisionNoGoRequest request) {
        User current = userService.getCurrentUser();
        ensureCi(current);
        CandidatureIndustrialisation candidature = getCandidature(id);
        ensureDecisionAllowed(candidature);
        if (!StringUtils.hasText(request.motif())) {
            throw new BusinessException("Le motif est obligatoire pour une decision No Go.");
        }

        StatutIndustrialisation oldStatus = candidature.getStatut();
        candidature.setStatut(StatutIndustrialisation.NO_GO);
        candidature.setDecisionGoNoGo(false);
        candidature.setMotifDecision(request.motif().trim());
        candidature.setCiDecideur(current);
        candidature.setDateDecisionCI(java.time.LocalDateTime.now());
        CandidatureIndustrialisation saved = candidatureRepository.save(candidature);
        addHistory(saved, oldStatus, StatutIndustrialisation.NO_GO, current, request.motif(), null);
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

    private void ensureOwner(User current, SujetProjet projet) {
        if (current.getRole() != Role.ROLE_ENSEIGNANT || !projet.getEncadrantId().equals(current.getId())) {
            throw new AccessDeniedException("Seul l'encadrant du projet peut gerer cette demande.");
        }
    }

    private void ensureCanRead(User current, CandidatureIndustrialisation candidature) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        if (candidature.getSujetProjet().getEncadrantId().equals(current.getId())) {
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

    private void addHistory(CandidatureIndustrialisation candidature,
                            StatutIndustrialisation source,
                            StatutIndustrialisation target,
                            User actor,
                            String motif,
                            String commentaire) {
        HistoriqueIndustrialisation history = new HistoriqueIndustrialisation();
        history.setCandidatureIndustrialisation(candidature);
        history.setStatutSource(source);
        history.setStatutCible(target);
        history.setActeur(actor);
        history.setRoleActeur(actor.getRole().name());
        history.setMotif(trimToNull(motif));
        history.setCommentaire(trimToNull(commentaire));
        historiqueRepository.save(history);
    }

    private CandidatureIndustrialisationResponse toResponse(CandidatureIndustrialisation candidature, boolean includeDetails) {
        SujetProjet projet = candidature.getSujetProjet();
        User demandeur = candidature.getDemandeur();
        User decideur = candidature.getCiDecideur();
        List<ReponseIndustrialisationResponse> reponses = includeDetails
                ? reponseRepository.findByCandidatureIndustrialisationId(candidature.getId())
                    .stream()
                    .sorted(Comparator.comparing(r -> r.getQuestion().getOrdre()))
                    .map(this::toReponseResponse)
                    .toList()
                : List.of();
        List<LivrableResponse> livrables = includeDetails ? livrableService.findByProjet(projet.getId()) : List.of();
        List<HistoriqueIndustrialisationResponse> historique = includeDetails
                ? historiqueRepository.findByCandidatureIndustrialisationIdOrderByDateTransitionAsc(candidature.getId())
                    .stream()
                    .map(this::toHistoriqueResponse)
                    .toList()
                : List.of();
        EvaluationResponse latestEvaluation = includeDetails ? findLatestEvaluation(projet.getId()) : null;

        return new CandidatureIndustrialisationResponse(
                candidature.getId(),
                projet.getId(),
                projet.getTitre(),
                projet.getStatut(),
                projet.getCategorie(),
                projet.getDomaine(),
                projet.getDescription(),
                projet.getObjectifs(),
                projet.getTechnologies(),
                projet.getPrerequis(),
                projet.getDateCreation(),
                projet.getDateSoumission(),
                projet.getDateValidation(),
                projet.getDateTerminaison(),
                projet.getScoreFinalEvaluation(),
                projet.getEligibleIndustrialisation(),
                projet.getBloqueParEliminatoire(),
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
                latestEvaluation
        );
    }

    private EvaluationResponse findLatestEvaluation(Long projetId) {
        try {
            return evaluationService.getLatestEvaluationByProjet(projetId);
        } catch (tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException ex) {
            return null;
        }
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

    private HistoriqueIndustrialisationResponse toHistoriqueResponse(HistoriqueIndustrialisation history) {
        User actor = history.getActeur();
        return new HistoriqueIndustrialisationResponse(
                history.getId(),
                history.getStatutSource(),
                history.getStatutCible(),
                actor.getId(),
                actor.getPrenom() + " " + actor.getNom(),
                history.getRoleActeur(),
                history.getDateTransition(),
                history.getMotif(),
                history.getCommentaire()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
