package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock private JwtAuthenticationFilter jwtFilter;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private CorsConfigurationSource corsConfigSource;

    @Test
    void passwordEncoder_returnsBCrypt() {
        SecurityConfig config = new SecurityConfig(jwtFilter, userDetailsService, corsConfigSource);

        PasswordEncoder encoder = config.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    void authenticationProvider_configured() {
        SecurityConfig config = new SecurityConfig(jwtFilter, userDetailsService, corsConfigSource);

        DaoAuthenticationProvider provider = config.authenticationProvider();

        assertNotNull(provider);
    }

    @Test
    void authenticationManager_delegatesToConfiguration() throws Exception {
        SecurityConfig config = new SecurityConfig(jwtFilter, userDetailsService, corsConfigSource);
        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager result = config.authenticationManager(authConfig);

        assertEquals(mockManager, result);
    }
}
