package tn.esprit.backend_esprittech.service.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.dashboard.DashboardResponse;
import tn.esprit.backend_esprittech.dto.dashboard.extended.*;
import tn.esprit.backend_esprittech.entity.historique.Historique;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.*;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;
import tn.esprit.backend_esprittech.repository.*;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.equipe.AffiliationEnseignantRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.historique.HistoriqueRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<StatutSujet> SUJET_STATUTS_EN_ATTENTE =
            List.of(StatutSujet.SOUMIS_EN_VALIDATION, StatutSujet.EN_ATTENTE);

    private static final Map<ActionType, String> ACTIVITY_TYPE_MAP = Map.ofEntries(
            Map.entry(ActionType.VALIDATE, "validation"),
            Map.entry(ActionType.INVALIDATE, "refus"),
            Map.entry(ActionType.ACCEPT, "acceptation"),
            Map.entry(ActionType.REFUSE, "refus"),
            Map.entry(ActionType.DECISION_GO, "validation"),
            Map.entry(ActionType.DECISION_NOGO, "refus"),
            Map.entry(ActionType.CREATE, "creation"),
            Map.entry(ActionType.DEMANDER_AFFILIATION, "en_attente"),
            Map.entry(ActionType.TRAITER_AFFILIATION, "validation"),
            Map.entry(ActionType.AJOUTER_MEMBRE, "validation"),
            Map.entry(ActionType.RETIRER_MEMBRE, "refus"),
            Map.entry(ActionType.SUBMIT, "en_attente")
    );

    private static final Map<Role, String> ROLE_FRENCH_MAP = Map.of(
            Role.ROLE_ETUDIANT, "Étudiant",
            Role.ROLE_ENSEIGNANT, "Enseignant",
            Role.ROLE_CHEF_EQUIPE, "Chef d'équipe",
            Role.ROLE_ADMIN, "Administrateur",
            Role.ROLE_CI, "CI"
    );

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

    private final SujetProjetRepository sujetProjetRepository;
    private final CandidatureRepository candidatureRepository;
    private final ProjetCatalogueRepository projetCatalogueRepository;
    private final CandidatureIndustrialisationRepository candidatureIndustrialisationRepository;
    private final UserRepository userRepository;
    private final EquipeRechercheRepository equipeRechercheRepository;
    private final AffiliationEnseignantRepository affiliationEnseignantRepository;
    private final HistoriqueRepository historiqueRepository;

    public DashboardResponse getDashboard() {
        long sujetsTotal = sujetProjetRepository.count();
        long sujetsInvalides = sujetProjetRepository
                .findByStatutOrderByDateCreationDesc(StatutSujet.INVALIDE).size();
        long sujetsEnAttente = sujetProjetRepository
                .findByStatutInOrderByDateCreationDesc(SUJET_STATUTS_EN_ATTENTE).size();
        long sujetsValides = sujetsTotal - sujetsInvalides - sujetsEnAttente;

        long candidaturesTotal = candidatureRepository.count();
        long candidaturesEnAttente = candidatureRepository.countByStatut(StatutCandidature.DEPOSEE);
        long candidaturesAcceptees = candidatureRepository.countByStatut(StatutCandidature.ACCEPTEE);
        long candidaturesRefusees = candidatureRepository.countByStatut(StatutCandidature.REFUSEE);

        long projetsAuCatalogue = projetCatalogueRepository
                .findByStatutInOrderByDateCreationDesc(StatutProjet.STATUTS_CATALOGUE).size();
        long projetsEnAttenteValidationCatalogue = projetCatalogueRepository
                .countByStatut(StatutProjet.SOUMIS_EN_VALIDATION);

        long industrialisationsGo = candidatureIndustrialisationRepository.countByDecisionGoNoGoTrue();
        long industrialisationsNoGo = candidatureIndustrialisationRepository.countByDecisionGoNoGoFalse();

        Double avg = candidatureIndustrialisationRepository.averageScoreEvaluationProjet();
        double scoreMoyen = avg == null ? 0.0 : Math.round(avg * 100.0) / 100.0;

        return new DashboardResponse(
                sujetsTotal, sujetsValides, sujetsInvalides, sujetsEnAttente,
                candidaturesTotal, candidaturesEnAttente, candidaturesAcceptees, candidaturesRefusees,
                projetsAuCatalogue, projetsEnAttenteValidationCatalogue,
                industrialisationsGo, industrialisationsNoGo, scoreMoyen
        );
    }

    @Transactional(readOnly = true)
    public ExtendedDashboardResponse getExtendedDashboard() {
        AdminKpiResponse kpis = buildKpis();
        List<TrendItem> trendsSujets = buildTrendItems(sujetProjetRepository.countSujetsByMonth(), 12);
        List<TrendItem> trendsCandidatures = buildTrendItems(candidatureRepository.countCandidaturesByMonth(), 7);
        List<TrendItem> trendsEquipes = buildTrendItems(equipeRechercheRepository.countEquipesByMonth(), 12);
        List<SujetByStatutItem> sujetsByStatut = buildSujetsByStatut();
        List<UsersByRoleItem> usersByRole = buildUsersByRole();
        List<CatalogueByDomainItem> catalogueByDomain = buildCatalogueByDomain();
        List<RecentActivityItem> recentActivity = buildRecentActivity();

        return new ExtendedDashboardResponse(
                kpis, trendsSujets, trendsCandidatures, trendsEquipes,
                sujetsByStatut, usersByRole, catalogueByDomain, recentActivity
        );
    }

    private AdminKpiResponse buildKpis() {
        long usersTotal = userRepository.count();
        long encadrantsTotal = userRepository.countByRole(Role.ROLE_ENSEIGNANT)
                + userRepository.countByRole(Role.ROLE_CHEF_EQUIPE);
        long equipesTotal = equipeRechercheRepository.count();
        long affiliationsEnAttente = affiliationEnseignantRepository.countByStatut(StatutAffiliation.EN_ATTENTE);

        long sujetsTotal = sujetProjetRepository.count();
        long sujetsInvalides = sujetProjetRepository.countByStatut(StatutSujet.INVALIDE);
        long sujetsEnAttente = sujetProjetRepository.countByStatut(StatutSujet.SOUMIS_EN_VALIDATION)
                + sujetProjetRepository.countByStatut(StatutSujet.EN_ATTENTE);
        long sujetsValides = sujetsTotal - sujetsInvalides - sujetsEnAttente;

        long candidaturesEnAttente = candidatureRepository.countByStatut(StatutCandidature.DEPOSEE);
        long candidaturesAcceptees = candidatureRepository.countByStatut(StatutCandidature.ACCEPTEE);
        long candidaturesRefusees = candidatureRepository.countByStatut(StatutCandidature.REFUSEE);

        long projetsCatalogue = projetCatalogueRepository
                .findByStatutInOrderByDateCreationDesc(StatutProjet.STATUTS_CATALOGUE).size();

        long industrialisationsGo = candidatureIndustrialisationRepository.countByDecisionGoNoGoTrue();
        long industrialisationsNoGo = candidatureIndustrialisationRepository.countByDecisionGoNoGoFalse();

        Double avg = candidatureIndustrialisationRepository.averageScoreEvaluationProjet();
        double scoreMoyen = avg == null ? 0.0 : Math.round(avg * 100.0) / 100.0;

        return new AdminKpiResponse(
                usersTotal, encadrantsTotal, equipesTotal, affiliationsEnAttente,
                sujetsTotal, sujetsEnAttente, sujetsValides, sujetsInvalides,
                candidaturesEnAttente, candidaturesAcceptees, candidaturesRefusees,
                projetsCatalogue, industrialisationsGo, industrialisationsNoGo, scoreMoyen
        );
    }

    private List<TrendItem> buildTrendItems(List<Object[]> rows, int limit) {
        Map<String, Long> monthMap = new LinkedHashMap<>();

        LocalDateTime now = LocalDateTime.now();
        for (int i = limit - 1; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String key = month.format(MONTH_FORMATTER);
            monthMap.put(key, 0L);
        }

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            LocalDateTime date = LocalDateTime.of(year, month, 1, 0, 0);
            String key = date.format(MONTH_FORMATTER);
            if (monthMap.containsKey(key)) {
                monthMap.put(key, monthMap.get(key) + count);
            }
        }

        return monthMap.entrySet().stream()
                .map(e -> new TrendItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<SujetByStatutItem> buildSujetsByStatut() {
        long enAttente = sujetProjetRepository.countByStatut(StatutSujet.SOUMIS_EN_VALIDATION)
                + sujetProjetRepository.countByStatut(StatutSujet.EN_ATTENTE);
        long invalides = sujetProjetRepository.countByStatut(StatutSujet.INVALIDE);
        long total = sujetProjetRepository.count();
        long valides = total - enAttente - invalides;

        return List.of(
                new SujetByStatutItem("En attente", enAttente, "#F59E0B"),
                new SujetByStatutItem("Validés", valides, "#22C55E"),
                new SujetByStatutItem("Invalidés", invalides, "#EF4444")
        );
    }

    private List<UsersByRoleItem> buildUsersByRole() {
        return Arrays.stream(Role.values())
                .map(r -> new UsersByRoleItem(
                        ROLE_FRENCH_MAP.getOrDefault(r, r.name()),
                        userRepository.countByRole(r)
                ))
                .collect(Collectors.toList());
    }

    private List<CatalogueByDomainItem> buildCatalogueByDomain() {
        List<Object[]> rows = projetCatalogueRepository.countByDomaine();
        return rows.stream()
                .map(row -> new CatalogueByDomainItem((String) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    private List<RecentActivityItem> buildRecentActivity() {
        List<Historique> history = historiqueRepository.findTop15ByOrderByCreatedAtDesc();
        return history.stream().map(h -> {
            String actorName = h.getActor().getPrenom() + " " + h.getActor().getNom();
            String message = h.getSummary() != null && !h.getSummary().isBlank()
                    ? h.getSummary()
                    : h.getAction().name() + " " + h.getEntityType().name().toLowerCase();
            String relativeTime = computeRelativeTime(h.getCreatedAt());
            String type = ACTIVITY_TYPE_MAP.getOrDefault(h.getAction(), "en_attente");
            return new RecentActivityItem(h.getId(), message, actorName, relativeTime, type);
        }).collect(Collectors.toList());
    }

    private String computeRelativeTime(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "À l'instant";
        long minutes = seconds / 60;
        if (minutes < 60) return "Il y a " + minutes + " min";
        long hours = minutes / 60;
        if (hours < 24) return "Il y a " + hours + "h";
        long days = hours / 24;
        if (days < 30) return "Il y a " + days + "j";
        long months = days / 30;
        if (months < 12) return "Il y a " + months + "mois";
        return "Il y a " + (months / 12) + "ans";
    }
}
