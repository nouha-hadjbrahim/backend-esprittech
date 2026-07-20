package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.dashboard.extended.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExtendedDashboardResponseTest {

    @Test
    void recordAccessors_returnValues() {
        AdminKpiResponse kpis = new AdminKpiResponse(
                50L, 20L, 8L, 2L,
                10L, 5L, 3L, 2L,
                5L, 12L, 3L,
                4L, 6L, 2L, 78.46
        );
        List<TrendItem> trends = List.of(new TrendItem("janv. 2026", 5));
        List<SujetByStatutItem> sujets = List.of(new SujetByStatutItem("En attente", 5, "#F59E0B"));
        List<UsersByRoleItem> roles = List.of(new UsersByRoleItem("Étudiant", 30));
        List<CatalogueByDomainItem> domaines = List.of(new CatalogueByDomainItem("Info", 4));
        List<RecentActivityItem> activity = List.of(new RecentActivityItem(1L, "msg", "actor", "now", "validation"));

        ExtendedDashboardResponse response = new ExtendedDashboardResponse(
                kpis, trends, trends, trends, trends,
                sujets, List.of(new SujetByCategorieItem("PFE", 3, "#E23E3E")), roles, domaines, domaines, activity
        );

        assertEquals(kpis, response.kpis());
        assertEquals(trends, response.trendsSujets());
        assertEquals(trends, response.trendsCandidatures());
        assertEquals(trends, response.trendsIndustrialisation());
        assertEquals(trends, response.trendsEquipes());
        assertEquals(sujets, response.sujetsByStatut());
        assertEquals(1, response.sujetsByCategorie().size());
        assertEquals(roles, response.usersByRole());
        assertEquals(domaines, response.catalogueByDomain());
        assertEquals(domaines, response.sujetsByDomaine());
        assertEquals(activity, response.recentActivity());
    }

    @Test
    void record_withEmptyLists() {
        AdminKpiResponse kpis = new AdminKpiResponse(
                0L, 0L, 0L, 0L,
                0L, 0L, 0L, 0L,
                0L, 0L, 0L,
                0L, 0L, 0L, 0.0
        );

        ExtendedDashboardResponse response = new ExtendedDashboardResponse(
                kpis,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );

        assertNotNull(response.kpis());
        assertEquals(0, response.trendsSujets().size());
        assertEquals(0, response.recentActivity().size());
    }
}
