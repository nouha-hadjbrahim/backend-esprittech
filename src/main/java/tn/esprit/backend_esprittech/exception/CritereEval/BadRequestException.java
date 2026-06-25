package tn.esprit.backend_esprittech.exception.CritereEval;


public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}