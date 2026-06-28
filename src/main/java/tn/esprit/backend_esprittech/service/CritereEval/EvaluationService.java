package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
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
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final UserService userService;

    public EvaluationResponse calculerEvaluation(Long projetId, EvaluationRequest request) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        EvaluationData data = buildManualEvaluation(request);
        return persistEvaluation(projet, data, resolveActorName(userService.getCurrentUser()), null);
    }

    public EvaluationResponse calculateAutomaticEvaluation(Long projetId, String recalculationReason) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        User currentUser = userService.getCurrentUser();
        ensureCanCalculate(currentUser, projet);
        EvaluationData data = buildAutomaticEvaluationFromRules(projet);
        String effectiveReason = currentUser.getRole() == Role.ROLE_ENSEIGNANT
                ? "Nouveaux livrables déposés"
                : recalculationReason;
        return persistEvaluation(projet, data, resolveActorName(currentUser), effectiveReason);
    }

    public EvaluationResponse calculateEvaluationForSystem(Long projetId) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        EvaluationData data = buildAutomaticEvaluationFromRules(projet);
        return persistEvaluation(projet, data, "SYSTEM", "Calcul automatique après terminaison du projet");
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getLatestEvaluationByProjet(Long projetId) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));
        ensureCanView(userService.getCurrentUser(), projet);

        Evaluation evaluation = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune évaluation trouvée pour le projet id: " + projetId));

        return toResponse(evaluation);
    }

    private SujetProjet getProjetAndValidateTerminated(Long projetId) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));

        if (projet.getStatut() != StatutSujet.REALISATION_TERMINEE) {
            throw new BadRequestException("Le projet doit être en statut REALISATION_TERMINEE pour être évalué.");
        }
        return projet;
    }

    private EvaluationData buildManualEvaluation(EvaluationRequest request) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();

        Map<Long, ResultatEliminatoireRequest> resultatsEliminatoires = mapEliminatoires(request.eliminatoires());
        Map<Long, ResultatNoteRequest> resultatsNotes = mapNotes(request.notes());

        List<ResultatCritere> resultats = new ArrayList<>();
        boolean bloqueParEliminatoire = false;
        double sommeScoresPonderes = 0.0;
        double sommePoids = 0.0;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            ResultatEliminatoireRequest resultatRequest = resultatsEliminatoires.get(critere.getId());
            if (resultatRequest == null) {
                throw new BadRequestException("Résultat manquant pour le critère éliminatoire id: " + critere.getId());
            }

            ResultatCritere resultat = baseResult(critere, TypeCritere.ELIMINATOIRE);
            resultat.setModeEvaluation(ModeEvaluation.MANUAL_ADMIN_VALUE);
            resultat.setRuleConfigured(true);
            resultat.setReponseEliminatoire(resultatRequest.reponse());
            resultat.setEvidenceSummary("Valeur saisie manuellement.");
            resultat.setCommentaire(resultatRequest.commentaire());
            resultats.add(resultat);

            if (resultatRequest.reponse() == ReponseEliminatoire.NOT_OK) {
                bloqueParEliminatoire = true;
            }
        }

        for (CritereNote critere : criteresNotes) {
            ResultatNoteRequest resultatRequest = resultatsNotes.get(critere.getId());
            if (resultatRequest == null) {
                throw new BadRequestException("Résultat manquant pour le critère noté id: " + critere.getId());
            }

            validateNoteObtenue(resultatRequest.noteObtenue(), critere);
            double scorePondere = (resultatRequest.noteObtenue() / critere.getBareme()) * critere.getPoids();
            sommeScoresPonderes += scorePondere;
            sommePoids += critere.getPoids();

            ResultatCritere resultat = baseResult(critere, TypeCritere.NOTE);
            resultat.setModeEvaluation(ModeEvaluation.MANUAL_ADMIN_VALUE);
            resultat.setRuleConfigured(true);
            resultat.setNoteObtenue(resultatRequest.noteObtenue());
            resultat.setScorePondere(scorePondere);
            resultat.setBareme(critere.getBareme());
            resultat.setPoids(critere.getPoids());
            resultat.setEvidenceSummary("Valeur saisie manuellement.");
            resultat.setCommentaire(resultatRequest.commentaire());
            resultats.add(resultat);
        }

        return buildEvaluationData(
                resultats,
                bloqueParEliminatoire,
                sommeScoresPonderes,
                sommePoids,
                "Projet évalué avec succès.",
                "Projet non éligible : au moins un critère éliminatoire est NOT_OK."
        );
    }

    private EvaluationData buildAutomaticEvaluationFromRules(SujetProjet projet) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();
        List<Livrable> livrables = livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(projet.getId());

        List<ResultatCritere> resultats = new ArrayList<>();
        boolean bloqueParEliminatoire = false;
        double sommeScoresPonderes = 0.0;
        double sommePoids = 0.0;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            ResultatCritere resultat = baseResult(critere, TypeCritere.ELIMINATOIRE);
            if (!isRuleConfigured(critere)) {
                markUnconfigured(resultat);
                resultats.add(resultat);
                continue;
            }

            RuleOutcome outcome = evaluateRule(critere, livrables);
            ReponseEliminatoire resultatAuto = outcome.satisfied() ? ReponseEliminatoire.OK : ReponseEliminatoire.NOT_OK;
            resultat.setRuleConfigured(true);
            resultat.setModeEvaluation(critere.getModeEvaluation());
            resultat.setReponseEliminatoire(resultatAuto);
            resultat.setEvidenceSummary(outcome.evidenceSummary());
            resultat.setEvidenceLivrableIds(joinLivrableIds(outcome.matchedLivrables()));
            resultat.setCommentaire(StringUtils.hasText(critere.getRuleDescription())
                    ? critere.getRuleDescription()
                    : "Regle automatique appliquee.");
            resultats.add(resultat);

            if (resultatAuto == ReponseEliminatoire.NOT_OK) {
                bloqueParEliminatoire = true;
            }
        }

        for (CritereNote critere : criteresNotes) {
            ResultatCritere resultat = baseResult(critere, TypeCritere.NOTE);
            resultat.setBareme(critere.getBareme());
            resultat.setPoids(critere.getPoids());

            if (!isRuleConfigured(critere)) {
                markUnconfigured(resultat);
                resultats.add(resultat);
                continue;
            }

            RuleOutcome outcome = evaluateRule(critere, livrables);
            double noteObtenue = calculateAutomaticNote(critere, outcome);
            validateNoteObtenue(noteObtenue, critere);
            double bareme = critere.getBareme() == null ? 0.0 : critere.getBareme();
            double scorePondere = bareme == 0.0 ? 0.0 : (noteObtenue / bareme) * critere.getPoids();

            sommeScoresPonderes += scorePondere;
            sommePoids += critere.getPoids();

            resultat.setRuleConfigured(true);
            resultat.setModeEvaluation(critere.getModeEvaluation());
            resultat.setNoteObtenue(round(noteObtenue));
            resultat.setScorePondere(round(scorePondere));
            resultat.setEvidenceSummary(outcome.evidenceSummary());
            resultat.setEvidenceLivrableIds(joinLivrableIds(outcome.matchedLivrables()));
            resultat.setCommentaire(StringUtils.hasText(critere.getRuleDescription())
                    ? critere.getRuleDescription()
                    : "Note calculee automatiquement par regle explicite.");
            resultats.add(resultat);
        }

        return buildEvaluationData(
                resultats,
                bloqueParEliminatoire,
                sommeScoresPonderes,
                sommePoids,
                "Projet evalue automatiquement avec des regles explicites.",
                "Projet non eligible : au moins un critere eliminatoire est NOT_OK."
        );
    }

    private EvaluationData buildAutomaticEvaluation(SujetProjet projet) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();
        List<Livrable> livrables = livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(projet.getId());

        List<ResultatCritere> resultats = new ArrayList<>();
        boolean bloqueParEliminatoire = false;
        double sommeScoresPonderes = 0.0;
        double sommePoids = 0.0;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            long matchingLivrables = countMatchingLivrables(livrables, critere.getDomaine(), critere.getLibelle(), critere.getDescription());
            ReponseEliminatoire resultatAuto = matchingLivrables > 0 ? ReponseEliminatoire.OK : ReponseEliminatoire.NOT_OK;

            ResultatCritere resultat = baseResult(critere.getId(), critere.getLibelle(), critere.getDomaine(), TypeCritere.ELIMINATOIRE);
            resultat.setReponseEliminatoire(resultatAuto);
            resultat.setCommentaire(matchingLivrables > 0
                    ? "Critère satisfait par " + matchingLivrables + " livrable(s) pertinent(s)."
                    : "Aucun livrable pertinent trouvé pour ce critère.");
            resultats.add(resultat);

            if (resultatAuto == ReponseEliminatoire.NOT_OK) {
                bloqueParEliminatoire = true;
            }
        }

        for (CritereNote critere : criteresNotes) {
            long matchingLivrables = countMatchingLivrables(livrables, critere.getDomaine(), critere.getLibelle(), critere.getDescription());
            double seuil = critere.getSeuil() == null || critere.getSeuil() <= 0 ? 1.0 : critere.getSeuil();
            double bareme = critere.getBareme() == null ? 0.0 : critere.getBareme();
            double noteObtenue = Math.min(bareme, (matchingLivrables / seuil) * bareme);
            validateNoteObtenue(noteObtenue, critere);
            double scorePondere = bareme == 0.0 ? 0.0 : (noteObtenue / bareme) * critere.getPoids();

            sommeScoresPonderes += scorePondere;
            sommePoids += critere.getPoids();

            ResultatCritere resultat = baseResult(critere.getId(), critere.getLibelle(), critere.getDomaine(), TypeCritere.NOTE);
            resultat.setNoteObtenue(round(noteObtenue));
            resultat.setScorePondere(round(scorePondere));
            resultat.setCommentaire("Note calculée automatiquement à partir de " + matchingLivrables + " livrable(s) pertinent(s).");
            resultats.add(resultat);
        }

        return buildEvaluationData(
                resultats,
                bloqueParEliminatoire,
                sommeScoresPonderes,
                sommePoids,
                "Projet évalué automatiquement à partir des livrables actifs.",
                "Projet non éligible : au moins un critère éliminatoire est NOT_OK."
        );
    }

    private EvaluationData buildEvaluationData(List<ResultatCritere> resultats,
                                               boolean bloqueParEliminatoire,
                                               double sommeScoresPonderes,
                                               double sommePoids,
                                               String commentaireOk,
                                               String commentaireKo) {
        double scoreFinal = sommePoids > 0 ? round((sommeScoresPonderes / sommePoids) * 100) : 0.0;
        boolean eligibleIndustrialisation = !bloqueParEliminatoire;
        String commentaire = bloqueParEliminatoire ? commentaireKo : commentaireOk;
        return new EvaluationData(resultats, scoreFinal, eligibleIndustrialisation, bloqueParEliminatoire, commentaire);
    }

    private EvaluationResponse persistEvaluation(SujetProjet projet,
                                                 EvaluationData data,
                                                 String calculatedBy,
                                                 String recalculationReason) {
        Evaluation evaluation = new Evaluation();
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(data.scoreFinal());
        evaluation.setEligibleIndustrialisation(data.eligibleIndustrialisation());
        evaluation.setBloqueParEliminatoire(data.bloqueParEliminatoire());
        evaluation.setCommentaire(data.commentaire());
        evaluation.setCalculatedBy(calculatedBy);
        evaluation.setRecalculationReason(recalculationReason);
        data.resultats().forEach(evaluation::addResultat);

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        projet.setScoreFinalEvaluation(data.scoreFinal());
        projet.setEligibleIndustrialisation(data.eligibleIndustrialisation());
        projet.setBloqueParEliminatoire(data.bloqueParEliminatoire());
        sujetProjetRepository.save(projet);

        return toResponse(savedEvaluation);
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
        resultat.setModeEvaluation(critere.getModeEvaluation());
        resultat.setRuleConfigured(isRuleConfigured(critere));
        return resultat;
    }

    private boolean isRuleConfigured(CritereEvaluation critere) {
        return Boolean.TRUE.equals(critere.getRuleEnabled()) && critere.getModeEvaluation() != null;
    }

    private void markUnconfigured(ResultatCritere resultat) {
        resultat.setRuleConfigured(false);
        resultat.setEvidenceSummary("Aucune regle automatique configuree pour ce critere.");
        resultat.setCommentaire("Ce critere necessite une regle d'evaluation automatique.");
    }

    private RuleOutcome evaluateRule(CritereEvaluation critere, List<Livrable> livrables) {
        ModeEvaluation mode = critere.getModeEvaluation();
        List<TypeLivrable> expectedTypes = expectedTypesFor(critere);
        int minCount = minCountFor(critere);
        String keyword = critere.getExpectedKeyword();
        List<Livrable> matches = switch (mode) {
            case DOCUMENTATION_PRESENT -> livrablesByTypes(livrables, expectedTypes);
            case GIT_LINK_PRESENT -> livrables.stream()
                    .filter(livrable -> hasType(livrable, expectedTypes) || containsIgnoreCase(livrable.getLienExterne(), "git"))
                    .toList();
            case LIVRABLE_TYPE_PRESENT -> livrablesByTypes(livrables, expectedTypes);
            case LIVRABLE_LINK_PRESENT -> livrables.stream()
                    .filter(livrable -> StringUtils.hasText(livrable.getLienExterne()))
                    .filter(livrable -> expectedTypes.isEmpty() || hasType(livrable, expectedTypes))
                    .toList();
            case LIVRABLE_FILE_PRESENT -> livrables.stream()
                    .filter(livrable -> StringUtils.hasText(livrable.getObjectName()))
                    .filter(livrable -> expectedTypes.isEmpty() || hasType(livrable, expectedTypes))
                    .toList();
            case LIVRABLE_COUNT_MIN -> livrables;
            case KEYWORD_IN_LIVRABLE_NAME_OR_DESCRIPTION -> livrables.stream()
                    .filter(livrable -> StringUtils.hasText(keyword) && searchableLivrableText(livrable).contains(keyword.toLowerCase(Locale.ROOT)))
                    .toList();
            case MANUAL_ADMIN_VALUE -> List.of();
        };

        boolean satisfied = switch (mode) {
            case LIVRABLE_COUNT_MIN -> livrables.size() >= minCount;
            case MANUAL_ADMIN_VALUE -> false;
            default -> !matches.isEmpty();
        };

        return new RuleOutcome(satisfied, matches, evidenceFor(mode, expectedTypes, minCount, keyword, matches, livrables.size()));
    }

    private double calculateAutomaticNote(CritereNote critere, RuleOutcome outcome) {
        double bareme = critere.getBareme() == null ? 0.0 : critere.getBareme();
        double maxAuto = critere.getNoteMaxAuto() == null || critere.getNoteMaxAuto() <= 0
                ? bareme
                : Math.min(critere.getNoteMaxAuto(), bareme);
        if (critere.getModeEvaluation() == ModeEvaluation.LIVRABLE_COUNT_MIN) {
            double minCount = minCountFor(critere);
            double matchedCount = outcome.matchedLivrables().size();
            return round(Math.min(maxAuto, (matchedCount / minCount) * maxAuto));
        }
        return outcome.satisfied() ? round(maxAuto) : 0.0;
    }

    private List<Livrable> livrablesByTypes(List<Livrable> livrables, List<TypeLivrable> types) {
        if (types.isEmpty()) {
            return livrables;
        }
        return livrables.stream()
                .filter(livrable -> hasType(livrable, types))
                .toList();
    }

    private boolean hasType(Livrable livrable, List<TypeLivrable> types) {
        return livrable.getTypeLivrable() != null && types.contains(livrable.getTypeLivrable());
    }

    private List<TypeLivrable> expectedTypesFor(CritereEvaluation critere) {
        List<TypeLivrable> configured = parseLivrableTypes(critere.getExpectedLivrableTypes());
        if (!configured.isEmpty()) {
            return configured;
        }
        if (critere.getModeEvaluation() == ModeEvaluation.DOCUMENTATION_PRESENT) {
            return List.of(TypeLivrable.DOCUMENTATION, TypeLivrable.RAPPORT);
        }
        if (critere.getModeEvaluation() == ModeEvaluation.GIT_LINK_PRESENT) {
            return List.of(TypeLivrable.LIEN_GIT, TypeLivrable.CODE_SOURCE);
        }
        return List.of();
    }

    private List<TypeLivrable> parseLivrableTypes(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split("[,;\\s]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> {
                    try {
                        return TypeLivrable.valueOf(value.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private int minCountFor(CritereEvaluation critere) {
        return critere.getMinLivrableCount() == null || critere.getMinLivrableCount() <= 0
                ? 1
                : critere.getMinLivrableCount();
    }

    private String evidenceFor(ModeEvaluation mode,
                               List<TypeLivrable> expectedTypes,
                               int minCount,
                               String keyword,
                               List<Livrable> matches,
                               int livrableCount) {
        if (mode == ModeEvaluation.LIVRABLE_COUNT_MIN) {
            Set<String> types = matches.stream()
                    .map(Livrable::getTypeLivrable)
                    .filter(Objects::nonNull)
                    .map(Enum::name)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            int missing = Math.max(0, minCount - livrableCount);
            String message = livrableCount + " livrable(s) actif(s) trouve(s) sur " + minCount + " requis"
                    + (types.isEmpty() ? "." : ". Types trouves: " + String.join(", ", types) + ".");
            return missing > 0
                    ? message + " Ajoutez au moins " + missing + " livrable(s) supplementaire(s) pour satisfaire ce critere."
                    : message;
        }
        if (matches.isEmpty()) {
            if (mode == ModeEvaluation.KEYWORD_IN_LIVRABLE_NAME_OR_DESCRIPTION) {
                return "Aucun livrable contenant le mot-cle \"" + safeValue(keyword) + "\" trouve.";
            }
            return "Aucun livrable correspondant trouve"
                    + (expectedTypes.isEmpty() ? "." : " pour les types " + joinTypes(expectedTypes) + ".");
        }
        if (mode == ModeEvaluation.GIT_LINK_PRESENT) {
            return "Lien Git ou code source trouve: " + displayLivrable(matches.get(0));
        }
        return matches.size() + " livrable(s) trouve(s): "
                + matches.stream().limit(3).map(this::displayLivrable).collect(Collectors.joining(", "));
    }

    private String displayLivrable(Livrable livrable) {
        if (StringUtils.hasText(livrable.getLienExterne())) {
            return livrable.getLienExterne();
        }
        if (StringUtils.hasText(livrable.getOriginalFileName())) {
            return livrable.getOriginalFileName();
        }
        return livrable.getNom();
    }

    private String joinTypes(List<TypeLivrable> types) {
        return types.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private String joinLivrableIds(List<Livrable> livrables) {
        return livrables.stream()
                .map(Livrable::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private boolean containsIgnoreCase(String source, String expected) {
        return StringUtils.hasText(source) && source.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private void ensureCanCalculate(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (current.getRole() == Role.ROLE_ENSEIGNANT && projet.getEncadrantId().equals(current.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous n'êtes pas autorisé à recalculer le score de ce projet.");
    }

    private void ensureCanView(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        if (current.getRole() == Role.ROLE_ENSEIGNANT && projet.getEncadrantId().equals(current.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous n'êtes pas autorisé à consulter cette évaluation.");
    }

    private long countMatchingLivrables(List<Livrable> livrables, String domaine, String libelle, String description) {
        if (livrables.isEmpty()) {
            return 0;
        }

        Set<String> keywords = extractKeywords(domaine, libelle, description);
        if (keywords.isEmpty()) {
            return livrables.size();
        }

        return livrables.stream()
                .map(this::searchableLivrableText)
                .filter(text -> keywords.stream().anyMatch(text::contains))
                .count();
    }

    private Set<String> extractKeywords(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .flatMap(value -> Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !Set.of("des", "les", "une", "pour", "avec", "dans", "sur", "and", "the").contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String searchableLivrableText(Livrable livrable) {
        return String.join(" ",
                safeLower(livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name()),
                safeLower(livrable.getNom()),
                safeLower(livrable.getDescription()),
                safeLower(livrable.getOriginalFileName()),
                safeLower(livrable.getLienExterne())
        );
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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

    private void validateNoteObtenue(Double noteObtenue, CritereNote critere) {
        if (noteObtenue == null) {
            throw new BadRequestException("La note obtenue est obligatoire pour le critère id: " + critere.getId());
        }
        if (critere.getBareme() == null || critere.getBareme() <= 0) {
            throw new BadRequestException("Le barème du critère doit être supérieur à 0.");
        }
        if (critere.getPoids() == null || critere.getPoids() <= 0) {
            throw new BadRequestException("Le poids du critère doit être supérieur à 0.");
        }
        if (noteObtenue < 0) {
            throw new BadRequestException("La note obtenue ne peut pas être négative.");
        }
        if (noteObtenue > critere.getBareme()) {
            throw new BadRequestException("La note obtenue ne peut pas dépasser le barème du critère id: " + critere.getId());
        }
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
                        resultat.getEvidenceSummary(),
                        parseEvidenceLivrableIds(resultat.getEvidenceLivrableIds()),
                        resultat.getRuleConfigured(),
                        resultat.getCommentaire()
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
        List<String> blockingCriteriaNames = resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> r.reponseEliminatoire() == ReponseEliminatoire.NOT_OK)
                .map(ResultatCritereResponse::critereLibelle)
                .toList();
        String coverageMessage = "Score calcule sur " + notesConfigures
                + " critere(s) note(s) configure(s) sur " + totalNotes + ".";
        if (notesNonConfigures > 0 || eliminatoiresNonConfigures > 0) {
            coverageMessage += " " + (notesNonConfigures + eliminatoiresNonConfigures)
                    + " critere(s) sont ignores ou incomplets car leurs regles automatiques ne sont pas configurees.";
        }

        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getSujetProjet().getId(),
                evaluation.getScoreFinal(),
                evaluation.getEligibleIndustrialisation(),
                evaluation.getBloqueParEliminatoire(),
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
                blockingCriteriaNames
        );
    }

    private record EvaluationData(
            List<ResultatCritere> resultats,
            Double scoreFinal,
            Boolean eligibleIndustrialisation,
            Boolean bloqueParEliminatoire,
            String commentaire
    ) {
    }

    private record RuleOutcome(
            boolean satisfied,
            List<Livrable> matchedLivrables,
            String evidenceSummary
    ) {
    }
}
