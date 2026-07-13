package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.dashboard.extended.AdminKpiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminKpiResponseTest {

    @Test
    void recordAccessors_returnValues() {
        AdminKpiResponse response = new AdminKpiResponse(
                50L, 20L, 8L, 2L,
                10L, 5L, 3L, 2L,
                5L, 12L, 3L,
                4L, 6L, 2L, 78.46
        );

        assertEquals(50L, response.usersTotal());
        assertEquals(20L, response.encadrantsTotal());
        assertEquals(8L, response.equipesTotal());
        assertEquals(2L, response.affiliationsEnAttente());
        assertEquals(10L, response.sujetsTotal());
        assertEquals(5L, response.sujetsEnAttente());
        assertEquals(3L, response.sujetsValides());
        assertEquals(2L, response.sujetsInvalides());
        assertEquals(5L, response.candidaturesEnAttente());
        assertEquals(12L, response.candidaturesAcceptees());
        assertEquals(3L, response.candidaturesRefusees());
        assertEquals(4L, response.projetsCatalogue());
        assertEquals(6L, response.industrialisationsGo());
        assertEquals(2L, response.industrialisationsNoGo());
        assertEquals(78.46, response.scoreMoyen());
    }

    @Test
    void record_toString_containsFields() {
        AdminKpiResponse response = new AdminKpiResponse(
                50L, 20L, 8L, 2L,
                10L, 5L, 3L, 2L,
                5L, 12L, 3L,
                4L, 6L, 2L, 78.46
        );

        String str = response.toString();
        assertEquals(true, str.contains("50"));
        assertEquals(true, str.contains("78.46"));
    }
}
