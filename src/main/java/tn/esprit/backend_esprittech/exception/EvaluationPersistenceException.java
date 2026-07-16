package tn.esprit.backend_esprittech.exception;

public class EvaluationPersistenceException extends RuntimeException {

    public static final String CODE = "EVALUATION_PERSISTENCE_FAILED";

    public EvaluationPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
