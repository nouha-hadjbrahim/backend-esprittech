package tn.esprit.backend_esprittech.config.sujetprojet;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SujetProjetSecurityConfigTest {

    @Test
    void sujetProjetSecurityFilterChain_buildsSuccessfully() throws Exception {
        SujetProjetSecurityConfig config = new SujetProjetSecurityConfig();
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(filterChain);

        SecurityFilterChain result = config.sujetProjetSecurityFilterChain(http);

        assertNotNull(result);
        assertEquals(filterChain, result);
        verify(http).securityMatcher("/api/sujet-projets/**");
    }
}
