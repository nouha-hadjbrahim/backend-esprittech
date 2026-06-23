package tn.esprit.backend_esprittech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fabrique les cookies {@code HttpOnly} portant les JWT (access + refresh).
 *
 * <p>Les tokens ne transitent plus par le corps JSON ni par le localStorage du
 * navigateur : ils sont déposés dans des cookies {@code HttpOnly}, donc
 * inaccessibles au JavaScript. Cela neutralise le vol de token via XSS.</p>
 *
 * <ul>
 *   <li>{@code secure} : {@code false} en dev (HTTP), {@code true} en prod (HTTPS) —
 *       piloté par {@code app.auth.cookie.secure}.</li>
 *   <li>{@code SameSite=Strict} : le cookie n'est pas envoyé sur les requêtes
 *       cross-site, ce qui protège contre le CSRF.</li>
 *   <li>L'access token est valable sur tout {@code /api/**} ; le refresh token est
 *       restreint à {@code /api/auth/refresh} pour limiter sa surface d'exposition.</li>
 * </ul>
 */
@Component
public class AuthCookieFactory {

    /** Cookie portant l'access token (envoyé sur tout /api/**). */
    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    /** Cookie portant le refresh token (restreint à l'endpoint de refresh). */
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private static final String ACCESS_PATH = "/api";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    private final boolean secure;
    private final String sameSite;
    private final Duration accessMaxAge;
    private final Duration refreshMaxAge;

    public AuthCookieFactory(
            JwtProperties jwtProperties,
            @Value("${app.auth.cookie.secure:false}") boolean secure,
            @Value("${app.auth.cookie.same-site:Strict}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.accessMaxAge = Duration.ofMillis(jwtProperties.getExpirationMs());
        this.refreshMaxAge = Duration.ofMillis(jwtProperties.getRefreshExpirationMs());
    }

    /** Cookie déposant l'access token, aligné sur la durée de vie du JWT. */
    public ResponseCookie accessToken(String token) {
        return build(ACCESS_TOKEN_COOKIE, token, ACCESS_PATH, accessMaxAge);
    }

    /** Cookie déposant le refresh token, aligné sur la durée de vie du refresh JWT. */
    public ResponseCookie refreshToken(String token) {
        return build(REFRESH_TOKEN_COOKIE, token, REFRESH_PATH, refreshMaxAge);
    }

    /** Cookie d'effacement (maxAge=0) de l'access token, pour le logout. */
    public ResponseCookie clearAccessToken() {
        return build(ACCESS_TOKEN_COOKIE, "", ACCESS_PATH, Duration.ZERO);
    }

    /** Cookie d'effacement (maxAge=0) du refresh token, pour le logout. */
    public ResponseCookie clearRefreshToken() {
        return build(REFRESH_TOKEN_COOKIE, "", REFRESH_PATH, Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
