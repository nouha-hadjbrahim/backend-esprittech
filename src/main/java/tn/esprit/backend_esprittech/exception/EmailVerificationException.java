package tn.esprit.backend_esprittech.exception;

/**
 * Levée lorsqu'une étape de vérification de l'email échoue : code invalide,
 * expiré, trop de tentatives, ou renvoi demandé trop tôt.
 */
public class EmailVerificationException extends RuntimeException {
    public EmailVerificationException(String message) {
        super(message);
    }
}
