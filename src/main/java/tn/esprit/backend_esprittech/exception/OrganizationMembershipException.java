package tn.esprit.backend_esprittech.exception;

/**
 * Levée lorsqu'un email fourni à l'inscription n'appartient pas à l'organisation
 * ESPRIT (aucun utilisateur correspondant dans Azure Entra ID).
 */
public class OrganizationMembershipException extends RuntimeException {
    public OrganizationMembershipException(String message) {
        super(message);
    }
}
