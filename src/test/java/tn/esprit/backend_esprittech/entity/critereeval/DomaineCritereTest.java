package tn.esprit.backend_esprittech.entity.critereeval;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class DomaineCritereTest {

    @Test
    void values_containsAllDomains() {
        DomaineCritere[] values = DomaineCritere.values();

        assertEquals(7, values.length);
        assertNotNull(DomaineCritere.valueOf("DOCUMENTATION"));
        assertNotNull(DomaineCritere.valueOf("TECHNIQUE"));
        assertNotNull(DomaineCritere.valueOf("SECURITE"));
        assertNotNull(DomaineCritere.valueOf("INNOVATION"));
        assertNotNull(DomaineCritere.valueOf("IMPACT_METIER"));
        assertNotNull(DomaineCritere.valueOf("QUALITE_CODE"));
        assertNotNull(DomaineCritere.valueOf("MAINTENABILITE"));
    }

    @Test
    void valueOf_documentation_returnsCorrectEnum() {
        DomaineCritere result = DomaineCritere.valueOf("DOCUMENTATION");
        assertEquals(DomaineCritere.DOCUMENTATION, result);
    }

    @Test
    void valueOf_technique_returnsCorrectEnum() {
        DomaineCritere result = DomaineCritere.valueOf("TECHNIQUE");
        assertEquals(DomaineCritere.TECHNIQUE, result);
    }

    @Test
    void valueOf_securite_returnsCorrectEnum() {
        DomaineCritere result = DomaineCritere.valueOf("SECURITE");
        assertEquals(DomaineCritere.SECURITE, result);
    }

    @Test
    void valueOf_innovation_returnsCorrectEnum() {
        DomaineCritere result = DomaineCritere.valueOf("INNOVATION");
        assertEquals(DomaineCritere.INNOVATION, result);
    }

    @Test
    void valueOf_impactMetier_returnsCorrectEnum() {
        DomaineCritere result = DomaineCritere.valueOf("IMPACT_METIER");
        assertEquals(DomaineCritere.IMPACT_METIER, result);
    }

    @Test
    void valueOf_qualiteCode_returnsCorrectEnum() {
        DomaineCritere result = DomaineCritere.valueOf("QUALITE_CODE");
        assertEquals(DomaineCritere.QUALITE_CODE, result);
    }

    @Test
    void valueOf_maintenabilite_returnsCorrectEnum() {
        DomaineCritere result = DomaineCritere.valueOf("MAINTENABILITE");
        assertEquals(DomaineCritere.MAINTENABILITE, result);
    }

    @Test
    void enumValues_canBeUsedInComparison() {
        DomaineCritere domaine = DomaineCritere.TECHNIQUE;
        assertSame(DomaineCritere.TECHNIQUE, domaine);
        assertNotSame(DomaineCritere.SECURITE, domaine);
    }

    @Test
    void enumValues_matchExpectedSet() {
        assertEquals(EnumSet.of(
                DomaineCritere.DOCUMENTATION,
                DomaineCritere.TECHNIQUE,
                DomaineCritere.SECURITE,
                DomaineCritere.INNOVATION,
                DomaineCritere.IMPACT_METIER,
                DomaineCritere.QUALITE_CODE,
                DomaineCritere.MAINTENABILITE
        ), EnumSet.allOf(DomaineCritere.class));
    }

    @Test
    void enumValues_allEnumsAreDifferent() {
        DomaineCritere[] values = DomaineCritere.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i], values[j]);
            }
        }
    }
}
