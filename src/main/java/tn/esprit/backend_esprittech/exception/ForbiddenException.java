package tn.esprit.backend_esprittech.exception;

/**
 * Levée lorsqu'un utilisateur authentifié n'est pas autorisé à effectuer une action
 * précise (ex. enseignant non affilié, chef d'une autre équipe). Mappée sur un HTTP 403
 * en conservant le message métier, contrairement à {@code AccessDeniedException} de Spring.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
