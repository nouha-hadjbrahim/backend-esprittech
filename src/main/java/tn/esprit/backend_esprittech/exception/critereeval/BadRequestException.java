package tn.esprit.backend_esprittech.exception.critereeval;


public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}