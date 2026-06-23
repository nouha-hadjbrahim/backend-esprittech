package tn.esprit.backend_esprittech.config.sujetprojet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Accès public aux endpoints SujetProjet pour les tests avec l'encadrant mock,
 * sans modifier la configuration de sécurité globale ni l'entité User.
 */
@Configuration
public class SujetProjetSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain sujetProjetSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/sujet-projets/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
