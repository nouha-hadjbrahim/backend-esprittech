package tn.esprit.backend_esprittech.config.sujetprojet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Accès public aux endpoints SujetProjet pour les tests avec l'encadrant mock,
 * sans modifier la configuration de sécurité globale ni l'entité User.
 */
@Configuration
public class SujetProjetSecurityConfig {

    private static final String SUJET_PROJET_API = "/api/sujet-projets/**";

    @Bean
    @Order(0)
    SecurityFilterChain sujetProjetSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(SUJET_PROJET_API)
                // API stateless sans cookie de session : CSRF ignoré uniquement sur ces routes JSON
                // (pas de credentials navigateur automatiques, donc pas de risque CSRF classique).
                .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher(SUJET_PROJET_API)))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
