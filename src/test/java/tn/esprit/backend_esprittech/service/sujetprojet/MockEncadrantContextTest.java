package tn.esprit.backend_esprittech.service.sujetprojet;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class MockEncadrantContextTest {

    @Test
    void constants_haveExpectedValues() {
        assertEquals(1001L, MockEncadrantContext.ENCADRANT_ID);
        assertEquals("Dr. Ben Ali Sami", MockEncadrantContext.NOM_COMPLET);
        assertEquals("Encadrant", MockEncadrantContext.ROLE);
        assertEquals("benali.sami@esprittech.tn", MockEncadrantContext.EMAIL);
        assertEquals("Informatique", MockEncadrantContext.DEPARTEMENT);
    }

    @Test
    void privateConstructor_canBeInvoked() throws Exception {
        Constructor<MockEncadrantContext> constructor = MockEncadrantContext.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
