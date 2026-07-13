package tn.esprit.backend_esprittech.dto.dashboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardResponseTest {

    @Test
    void recordAccessors_returnValues() {
        DashboardResponse response = new DashboardResponse(
                10L, 7L, 2L, 1L,
                20L, 5L, 12L, 3L,
                4L, 1L,
                6L, 2L, 78.46
        );

        assertEquals(10L, response.sujetsTotal());
        assertEquals(7L, response.sujetsValides());
        assertEquals(2L, response.sujetsInvalides());
        assertEquals(1L, response.sujetsEnAttente());
        assertEquals(20L, response.candidaturesTotal());
        assertEquals(5L, response.candidaturesEnAttente());
        assertEquals(12L, response.candidaturesAcceptees());
        assertEquals(3L, response.candidaturesRefusees());
        assertEquals(4L, response.projetsAuCatalogue());
        assertEquals(1L, response.projetsEnAttenteValidationCatalogue());
        assertEquals(6L, response.industrialisationsGo());
        assertEquals(2L, response.industrialisationsNoGo());
        assertEquals(78.46, response.scoreMoyen());
    }

    @Test
    void record_toString_containsFields() {
        DashboardResponse response = new DashboardResponse(
                10L, 7L, 2L, 1L,
                20L, 5L, 12L, 3L,
                4L, 1L,
                6L, 2L, 78.46
        );

        String str = response.toString();
        assertEquals(true, str.contains("10"));
        assertEquals(true, str.contains("78.46"));
    }
}
