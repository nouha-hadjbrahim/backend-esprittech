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
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNoteRule;
import tn.esprit.backend_esprittech.entity.CritereEval.Evaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ModeEvaluation;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.CritereEval.RuleOperator;
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
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRuleRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private static final int NOTE_MAX = 5;
    private static final int DEFAULT_NOTE = 3;

    private final SujetProjetRepository sujetProjetRepository;
    private final CritereEliminatoireRepository critereEliminatoireRepository;
    private final CritereNoteRepository critereNoteRepository;
    private final EvaluationRepository evaluationRepository;
    private final LivrableRepository livrableRepository;
    private final CritereNoteRuleRepository critereNoteRuleRepository;
    private final NoteLevelService noteLevelService;
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
                ? "Nouveaux livrables deposes"
                : recalculationReason;
        return persistEvaluation(projet, data, resolveActorName(currentUser), effectiveReason);
    }

    public EvaluationResponse calculateEvaluationForSystem(Long projetId) {
        SujetProjet projet = getProjetAndValidateTerminated(projetId);
        EvaluationData data = buildAutomaticEvaluationFromRules(projet);
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

    private SujetProjet getProjetAndValidateTerminated(Long projetId) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));

        if (projet.getStatut() != StatutSujet.REALISATION_TERMINEE) {
            throw new BadRequestException("Le projet doit etre en statut REALISATION_TERMINEE pour etre evalue.");
        }
        return projet;
    }

    private EvaluationData buildManualEvaluation(EvaluationRequest request) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();

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
            validateNoteValue(noteValue, critere);
            double poids = positiveWeight(critere.getPoids());
            double scoreCritere = scoreCritere(noteValue, poids);
            sommeScoresCriteres += scoreCritere;
            sommePoids += poids;

            ResultatCritere resultat = baseResult(critere, TypeCritere.NOTE);
            resultat.setModeEvaluation(ModeEvaluation.MANUAL_ADMIN_VALUE);
            resultat.setRuleConfigured(true);
            applyNoteResult(resultat, noteValue, poids, scoreCritere,
                    "MANUAL_ADMIN_VALUE", "Valeur saisie manuellement.", "Note saisie manuellement.");
            resultat.setCommentaire(resultatRequest.commentaire());
            resultats.add(resultat);
        }

        return buildEvaluationData(resultats, warnings, sommeScoresCriteres, sommePoids);
    }

    private EvaluationData buildAutomaticEvaluationFromRules(SujetProjet projet) {
        List<CritereEliminatoire> criteresEliminatoires = critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();
        List<CritereNote> criteresNotes = critereNoteRepository.findByActifTrueOrderByOrdreAsc();
        List<Livrable> livrables = livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(projet.getId());
        Map<String, Object> metadata = buildMetadata(projet, livrables);

        List<ResultatCritere> resultats = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        double sommeScoresCriteres = 0.0;
        double sommePoids = 0.0;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            ResultatCritere resultat = baseResult(critere, TypeCritere.ELIMINATOIRE);
            if (!isRuleConfigured(critere)) {
                markUnconfigured(resultat);
                resultats.add(resultat);
                continue;
            }

            RuleOutcome outcome = evaluateLegacyRule(critere, livrables);
            ReponseEliminatoire resultatAuto = outcome.satisfied() ? ReponseEliminatoire.OK : ReponseEliminatoire.NOT_OK;
            resultat.setRuleConfigured(true);
            resultat.setModeEvaluation(critere.getModeEvaluation());
            resultat.setReponseEliminatoire(resultatAuto);
            resultat.setEvidenceSummary(outcome.evidenceSummary());
            resultat.setEvidenceLivrableIds(joinLivrableIds(outcome.matchedLivrables()));
            resultat.setMetadataUsed(outcome.evidenceSummary());
            resultat.setExplanation(StringUtils.hasText(critere.getRuleDescription())
                    ? critere.getRuleDescription()
                    : "Regle eliminatoire automatique appliquee.");
            resultat.setCommentaire(resultat.getExplanation());
            resultats.add(resultat);

            if (resultatAuto == ReponseEliminatoire.NOT_OK) {
                warnings.add(critere.getLibelle());
            }
        }

        for (CritereNote critere : criteresNotes) {
            List<CritereNoteRule> rules = critereNoteRuleRepository
                    .findByCritereNoteIdAndActiveTrueOrderByPriorityAscIdAsc(critere.getId());
            NoteRuleOutcome outcome = evaluateNoteRules(critere, rules, metadata);
            int noteValue = outcome.noteValue();
            validateNoteValue(noteValue, critere);
            double poids = positiveWeight(critere.getPoids());
            double score = scoreCritere(noteValue, poids);
            sommeScoresCriteres += score;
            sommePoids += poids;

            ResultatCritere resultat = baseResult(critere, TypeCritere.NOTE);
            resultat.setRuleConfigured(!rules.isEmpty());
            resultat.setModeEvaluation(critere.getModeEvaluation());
            applyNoteResult(resultat, noteValue, poids, score,
                    outcome.matchedRule(), outcome.metadataUsed(), outcome.explanation());
            resultat.setCommentaire(outcome.explanation());
            resultats.add(resultat);
        }

        return buildEvaluationData(resultats, warnings, sommeScoresCriteres, sommePoids);
    }

    private EvaluationData buildEvaluationData(List<ResultatCritere> resultats,
                                               List<String> eliminatoryWarnings,
                                               double sommeScoresCriteres,
                                               double sommePoids) {
        double scoreFinal = sommePoids > 0 ? round((sommeScoresCriteres / sommePoids) * 100) : 0.0;
        boolean hasWarnings = !eliminatoryWarnings.isEmpty();
        String commentaire = hasWarnings
                ? "Projet evalue avec alertes eliminatoires non bloquantes."
                : "Projet evalue avec succes.";
        return new EvaluationData(resultats, scoreFinal, true, hasWarnings, eliminatoryWarnings, commentaire);
    }

    private EvaluationResponse persistEvaluation(SujetProjet projet,
                                                 EvaluationData data,
                                                 String calculatedBy,
                                                 String recalculationReason) {
        Evaluation evaluation = new Evaluation();
        evaluation.setSujetProjet(projet);
        evaluation.setScoreFinal(data.scoreFinal());
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
                                 String matchedRule,
                                 String metadataUsed,
                                 String explanation) {
        resultat.setNoteValue(noteValue);
        resultat.setNoteLabel(noteLevelService.labelFor(noteValue));
        resultat.setNoteObtenue((double) noteValue);
        resultat.setBareme((double) NOTE_MAX);
        resultat.setPoids(poids);
        resultat.setScorePondere(round(scoreCritere));
        resultat.setMatchedRule(matchedRule);
        resultat.setMetadataUsed(metadataUsed);
        resultat.setExplanation(explanation);
        resultat.setEvidenceSummary(metadataUsed);
    }

    private NoteRuleOutcome evaluateNoteRules(CritereNote critere,
                                              List<CritereNoteRule> rules,
                                              Map<String, Object> metadata) {
        for (CritereNoteRule rule : rules) {
            if (matchesRule(rule, metadata)) {
                return new NoteRuleOutcome(
                        rule.getNoteValue(),
                        rule.getRuleName(),
                        metadataSummary(rule.getMetadataKey(), metadata.get(rule.getMetadataKey())),
                        StringUtils.hasText(rule.getDescription())
                                ? rule.getDescription()
                                : "Regle metadata appliquee: " + rule.getRuleName()
                );
            }
        }
        int defaultNote = safeDefaultNote(critere);
        String explanation = rules.isEmpty()
                ? "Aucune regle active pour ce critere; note par defaut appliquee."
                : "Aucune regle metadata ne correspond; note par defaut appliquee.";
        return new NoteRuleOutcome(
                defaultNote,
                "DEFAULT_NOTE",
                metadataSummary("defaultNoteValue", defaultNote),
                explanation
        );
    }

    private boolean matchesRule(CritereNoteRule rule, Map<String, Object> metadata) {
        Object raw = metadata.get(rule.getMetadataKey());
        RuleOperator operator = rule.getOperator();
        return switch (operator) {
            case EXISTS -> exists(raw);
            case NOT_EXISTS -> !exists(raw);
            case EQUALS -> equalsExpected(raw, rule.getExpectedValue());
            case NOT_EQUALS -> !equalsExpected(raw, rule.getExpectedValue());
            case CONTAINS -> containsExpected(raw, rule.getExpectedValue());
            case GTE, COUNT_GTE -> numeric(raw) >= numericThreshold(rule);
            case LTE, COUNT_LTE -> numeric(raw) <= numericThreshold(rule);
            case BETWEEN -> numeric(raw) >= nullToDouble(rule.getMinValue())
                    && numeric(raw) <= nullToDouble(rule.getMaxValue());
        };
    }

    private Map<String, Object> buildMetadata(SujetProjet projet, List<Livrable> livrables) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        List<String> types = livrables.stream()
                .map(Livrable::getTypeLivrable)
                .filter(Objects::nonNull)
                .map(Enum::name)
                .distinct()
                .toList();
        List<String> extensions = livrables.stream()
                .map(this::extensionOf)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        metadata.put("livrableCount", livrables.size());
        metadata.put("livrableTypes", String.join(",", types));
        metadata.put("fileExtensions", String.join(",", extensions));
        metadata.put("fileCount", livrables.stream().filter(l -> StringUtils.hasText(l.getObjectName())).count());
        metadata.put("externalLinkCount", livrables.stream().filter(l -> StringUtils.hasText(l.getLienExterne())).count());
        metadata.put("hasGitLink", livrables.stream().anyMatch(this::isGitLivrable));
        metadata.put("hasDocumentation", livrables.stream().anyMatch(this::isDocumentationLivrable));
        metadata.put("hasPdfDocumentation", livrables.stream().anyMatch(this::isPdfDocumentationLivrable));
        metadata.put("hasImage", livrables.stream().anyMatch(this::isImageLivrable));
        metadata.put("totalFileSize", livrables.stream().map(Livrable::getSize).filter(Objects::nonNull).mapToLong(Long::longValue).sum());
        metadata.put("maxFileSize", livrables.stream().map(Livrable::getSize).filter(Objects::nonNull).mapToLong(Long::longValue).max().orElse(0));
        metadata.put("minFileSize", livrables.stream().map(Livrable::getSize).filter(Objects::nonNull).mapToLong(Long::longValue).min().orElse(0));
        metadata.put("latestUploadedAtEpoch", livrables.stream()
                .map(Livrable::getDateDepot)
                .filter(Objects::nonNull)
                .mapToLong(date -> date.toEpochSecond(ZoneOffset.UTC))
                .max()
                .orElse(0));
        metadata.put("projectStatus", projet.getStatut() == null ? "" : projet.getStatut().name());
        metadata.put("projectTitle", safeValue(projet.getTitre()));
        metadata.put("projectDomaine", safeValue(projet.getDomaine()));
        metadata.put("projectTechnologies", safeValue(projet.getTechnologies()));
        metadata.put("projectCategory", projet.getCategorie() == null ? "" : projet.getCategorie().name());
        metadata.put("livrableSearchText", livrables.stream().map(this::searchableLivrableText).collect(Collectors.joining(" ")));
        return metadata;
    }

    private boolean exists(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof Number number) {
            return number.doubleValue() > 0.0;
        }
        return StringUtils.hasText(String.valueOf(raw));
    }

    private boolean equalsExpected(Object raw, String expected) {
        if (raw == null || expected == null) {
            return raw == null && expected == null;
        }
        if (raw instanceof Boolean bool) {
            return bool == Boolean.parseBoolean(expected);
        }
        Double rawNumber = parseDouble(raw);
        Double expectedNumber = parseDouble(expected);
        if (rawNumber != null && expectedNumber != null) {
            return Double.compare(rawNumber, expectedNumber) == 0;
        }
        return String.valueOf(raw).equalsIgnoreCase(expected.trim());
    }

    private boolean containsExpected(Object raw, String expected) {
        if (raw == null || !StringUtils.hasText(expected)) {
            return false;
        }
        String source = String.valueOf(raw).toLowerCase(Locale.ROOT);
        return Arrays.stream(expected.split("[,;]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(source::contains);
    }

    private double numeric(Object raw) {
        Double value = parseDouble(raw);
        return value == null ? 0.0 : value;
    }

    private Double parseDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double numericThreshold(CritereNoteRule rule) {
        if (rule.getMinValue() != null) {
            return rule.getMinValue();
        }
        Double expected = parseDouble(rule.getExpectedValue());
        return expected == null ? 0.0 : expected;
    }

    private double nullToDouble(Double value) {
        return value == null ? 0.0 : value;
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
        resultat.setExplanation("Ce critere necessite une regle d'evaluation automatique.");
        resultat.setCommentaire(resultat.getExplanation());
    }

    private RuleOutcome evaluateLegacyRule(CritereEvaluation critere, List<Livrable> livrables) {
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
            int missing = Math.max(0, minCount - livrableCount);
            String message = livrableCount + " livrable(s) actif(s) trouve(s) sur " + minCount + " requis.";
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

    private boolean isGitLivrable(Livrable livrable) {
        return livrable.getTypeLivrable() == TypeLivrable.LIEN_GIT
                || livrable.getTypeLivrable() == TypeLivrable.CODE_SOURCE
                || containsIgnoreCase(livrable.getLienExterne(), "git");
    }

    private boolean isDocumentationLivrable(Livrable livrable) {
        return livrable.getTypeLivrable() == TypeLivrable.DOCUMENTATION
                || livrable.getTypeLivrable() == TypeLivrable.RAPPORT;
    }

    private boolean isPdfDocumentationLivrable(Livrable livrable) {
        return isDocumentationLivrable(livrable) && "pdf".equalsIgnoreCase(extensionOf(livrable));
    }

    private boolean isImageLivrable(Livrable livrable) {
        String extension = extensionOf(livrable);
        return livrable.getTypeLivrable() == TypeLivrable.IMAGE
                || Set.of("png", "jpg", "jpeg").contains(extension);
    }

    private String extensionOf(Livrable livrable) {
        String name = StringUtils.hasText(livrable.getOriginalFileName())
                ? livrable.getOriginalFileName()
                : livrable.getObjectName();
        if (!StringUtils.hasText(name) || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
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

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private String metadataSummary(String key, Object value) {
        return key + "=" + String.valueOf(value);
    }

    private int safeDefaultNote(CritereNote critere) {
        Integer configured = critere.getDefaultNoteValue();
        return configured == null || configured < 1 || configured > 5 ? DEFAULT_NOTE : configured;
    }

    private int resolveManualNoteValue(ResultatNoteRequest request) {
        if (request.noteValue() != null) {
            return request.noteValue();
        }
        if (request.noteObtenue() == null) {
            throw new BadRequestException("La note 1..5 est obligatoire pour le critere id: " + request.critereId());
        }
        return request.noteObtenue().intValue();
    }

    private void validateNoteValue(Integer noteValue, CritereNote critere) {
        if (noteValue == null) {
            throw new BadRequestException("La note est obligatoire pour le critere id: " + critere.getId());
        }
        if (noteValue < 1 || noteValue > NOTE_MAX) {
            throw new BadRequestException("La note doit etre comprise entre 1 et 5 pour le critere id: " + critere.getId());
        }
        if (critere.getPoids() == null || critere.getPoids() <= 0) {
            throw new BadRequestException("Le poids du critere doit etre superieur a 0.");
        }
    }

    private double positiveWeight(Double value) {
        return value == null || value <= 0.0 ? 1.0 : value;
    }

    private double scoreCritere(int noteValue, double poids) {
        return (noteValue / (double) NOTE_MAX) * poids;
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
        List<String> warningCriteriaNames = resultats.stream()
                .filter(r -> r.typeCritere() == TypeCritere.ELIMINATOIRE)
                .filter(r -> r.reponseEliminatoire() == ReponseEliminatoire.NOT_OK)
                .map(ResultatCritereResponse::critereLibelle)
                .toList();
        String coverageMessage = "Score calcule sur " + totalNotes
                + " critere(s) note(s) actif(s), avec note par defaut si aucune regle metadata ne correspond.";
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
                warningCriteriaNames
        );
    }

    private record EvaluationData(
            List<ResultatCritere> resultats,
            Double scoreFinal,
            Boolean eligibleIndustrialisation,
            Boolean hasEliminatoryWarnings,
            List<String> eliminatoryWarnings,
            String commentaire
    ) {
    }

    private record RuleOutcome(
            boolean satisfied,
            List<Livrable> matchedLivrables,
            String evidenceSummary
    ) {
    }

    private record NoteRuleOutcome(
            Integer noteValue,
            String matchedRule,
            String metadataUsed,
            String explanation
    ) {
    }
}
