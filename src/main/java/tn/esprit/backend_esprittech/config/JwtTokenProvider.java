package tn.esprit.backend_esprittech.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Génère et valide les JWT (access + refresh) avec la lib jjwt 0.12.x.
 * Le refresh token est ici un JWT signé (stratégie stateless) ; il porte un
 * claim {@code type=refresh} pour le distinguer d'un access token.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final int MIN_HS256_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(JwtProperties properties) {
        byte[] secretBytes = validateSecret(properties.getSecret());
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = properties.getExpirationMs();
    }

    public String generateAccessToken(User user) {
        return buildToken(user, TYPE_ACCESS, expirationMs);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, TYPE_REFRESH, expirationMs);
    }

    private byte[] validateSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing or empty. Set a secure random secret through the JWT_SECRET "
                            + "environment variable or create a local .env file from .env.example. "
                            + "The Spring property app.jwt.secret is resolved from ${JWT_SECRET:}.");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_HS256_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short for HS256. It must be at least "
                            + MIN_HS256_SECRET_BYTES + " UTF-8 bytes, but the configured value is "
                            + secretBytes.length + " bytes. Generate a secure random secret, set it as "
                            + "JWT_SECRET in your environment or local .env file, and do not commit the real value.");
        }
        return secretBytes;
    }

    private String buildToken(User user, String type, long ttlMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parse(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parse(token).get(CLAIM_TYPE, String.class));
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expiré : {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT invalide : {}", e.getMessage());
        }
        return false;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
