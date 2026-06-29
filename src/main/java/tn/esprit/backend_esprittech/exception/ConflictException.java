package tn.esprit.backend_esprittech.exception;

/**
 * Levée lorsqu'une action entre en conflit avec l'état courant de la ressource
 * (ex. valider un projet qui n'est plus en attente de validation). Mappée sur un HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
