package tn.esprit.backend_esprittech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de la vérification de propriété de l'email (code OTP) à l'inscription.
 */
@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(
        /** Durée de validité du code (secondes). */
        long codeTtlSeconds,
        /** Nombre maximal de tentatives de saisie avant invalidation du code. */
        int maxAttempts,
        /** Délai minimal (secondes) entre deux envois de code pour une même adresse. */
        long resendCooldownSeconds
) {
}
