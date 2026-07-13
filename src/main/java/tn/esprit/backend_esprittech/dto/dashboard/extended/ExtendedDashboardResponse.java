package tn.esprit.backend_esprittech.dto.dashboard.extended;

import java.util.List;

public record ExtendedDashboardResponse(
        AdminKpiResponse kpis,
        List<TrendItem> trendsSujets,
        List<TrendItem> trendsCandidatures,
        List<TrendItem> trendsEquipes,
        List<SujetByStatutItem> sujetsByStatut,
        List<UsersByRoleItem> usersByRole,
        List<CatalogueByDomainItem> catalogueByDomain,
        List<RecentActivityItem> recentActivity
) {}
