package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void corsConfigurationSource_configuredCorrectly() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", List.of("http://localhost:4200"));

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();

        assertInstanceOf(UrlBasedCorsConfigurationSource.class, source);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source).getCorsConfiguration(request);
        assertNotNull(config);
        assertEquals(List.of("http://localhost:4200"), config.getAllowedOrigins());
        assertTrue(config.getAllowCredentials());
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertEquals(List.of("Authorization", "X-XSRF-TOKEN"), config.getExposedHeaders());
    }
}
