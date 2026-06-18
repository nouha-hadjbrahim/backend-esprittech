package tn.esprit.backend_esprittech.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés JWT liées depuis {@code application.yml}, qui lui-même référence
 * les variables du fichier {@code .env} (chargé automatiquement par spring-dotenv).
 *
 * <p>Les secrets ne sont JAMAIS écrits en dur : application.yml fait
 * {@code jwt.secret: ${JWT_SECRET}} et la valeur provient du .env.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class DotenvConfig {

    /** Clé secrète HMAC (>= 32 octets pour HS256). Vient de JWT_SECRET. */
    private String secret;

    /** Durée de vie de l'access token en millisecondes (JWT_ACCESS_EXPIRATION). */
    private long accessExpiration;

    /** Durée de vie du refresh token en millisecondes (JWT_REFRESH_EXPIRATION). */
    private long refreshExpiration;
}
