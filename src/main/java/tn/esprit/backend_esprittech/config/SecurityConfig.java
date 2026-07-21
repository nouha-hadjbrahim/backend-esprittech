package tn.esprit.backend_esprittech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;

import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuration de sécurité : API stateless protégée par JWT.
 * Les annotations @PreAuthorize des controllers sont activées via @EnableMethodSecurity.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Dépôt du jeton CSRF selon le pattern « double-submit cookie ».
     *
     * <p><strong>HttpOnly est volontairement désactivé.</strong> L'intercepteur Angular
     * ({@code csrf.interceptor.ts}) lit le cookie {@code XSRF-TOKEN} via {@code document.cookie}
     * pour le réémettre dans l'en-tête {@code X-XSRF-TOKEN}. Rendre ce cookie HttpOnly le
     * rendrait illisible par le navigateur et désactiverait donc la protection CSRF.
     *
     * <p>Le risque reste borné : ce cookie ne transporte qu'un jeton anti-CSRF, jamais une
     * identité ni un secret de session. Les jetons d'authentification sont déposés dans des
     * cookies HttpOnly distincts (voir {@link AuthCookieFactory}), inaccessibles au JavaScript.
     * Les attributs {@code Secure} et {@code SameSite} suivent la configuration des cookies
     * d'authentification afin de rester cohérents entre les deux.
     */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(
            @Value("${app.auth.cookie.secure:false}") boolean secure,
            @Value("${app.auth.cookie.same-site:Strict}") String sameSite) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> cookie
                .secure(secure)
                .sameSite(sameSite)
                .path("/"));
        return repository;
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain filterChain(HttpSecurity http, CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new EagerCsrfTokenRequestHandler())
                        // Only the unauthenticated bootstrap endpoints are exempt: a client
                        // cannot present a CSRF token before it has obtained one. login/register
                        // establish the session, refresh/logout are guarded by the SameSite=Strict
                        // refresh-token cookie. Every authenticated /api/** mutation IS CSRF-checked.
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/auth/login"),
                                new AntPathRequestMatcher("/api/auth/register"),
                                new AntPathRequestMatcher("/api/auth/register/verify"),
                                new AntPathRequestMatcher("/api/auth/register/resend"),
                                new AntPathRequestMatcher("/api/auth/refresh"),
                                new AntPathRequestMatcher("/api/auth/logout")))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/register/verify",
                                "/api/auth/register/resend",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout").permitAll()
                        .requestMatchers("/api/accueil/stats").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Eagerly resolves the deferred CSRF token during the CsrfFilter.handle() call,
     * which runs for ALL requests (including CSRF-exempt auth endpoints).
     * Without this, the XSRF-TOKEN cookie is never written on exempt endpoints
     * because Spring Security 6 defers token resolution and only resolves it
     * when checking the token on non-exempt requests.
     */
    private static class EagerCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                Supplier<CsrfToken> deferredCsrfToken) {
            deferredCsrfToken.get(); // force resolution → triggers saveToken() → cookie set
            super.handle(request, response, deferredCsrfToken);
        }
    }
}
