package tn.esprit.backend_esprittech.exception;

/**
 * Levée lorsqu'une ressource demandée (équipe, utilisateur, affiliation) est introuvable.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
