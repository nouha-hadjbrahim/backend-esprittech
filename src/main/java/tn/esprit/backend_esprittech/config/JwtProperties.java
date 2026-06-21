package tn.esprit.backend_esprittech.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT settings bound from Spring configuration.
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * HMAC secret used by HS256. Must be at least 32 UTF-8 bytes.
     */
    private String secret;

    /**
     * Access token lifetime in milliseconds (short-lived, e.g. 15 min).
     */
    private long expirationMs = 900_000L;

    /**
     * Refresh token lifetime in milliseconds (long-lived, e.g. 7 days).
     */
    private long refreshExpirationMs = 604_800_000L;
}
