package tn.esprit.backend_esprittech.dto.dashboard.extended;

import java.util.List;

public record ExtendedDashboardResponse(
        AdminKpiResponse kpis,
        List<TrendItem> trendsSujets,
        List<TrendItem> trendsCandidatures,
        List<TrendItem> trendsIndustrialisation,
        List<TrendItem> trendsEquipes,
        List<SujetByStatutItem> sujetsByStatut,
        List<SujetByCategorieItem> sujetsByCategorie,
        List<UsersByRoleItem> usersByRole,
        List<CatalogueByDomainItem> catalogueByDomain,
        List<CatalogueByDomainItem> sujetsByDomaine,
        List<RecentActivityItem> recentActivity
) {}
