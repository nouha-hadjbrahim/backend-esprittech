package tn.esprit.backend_esprittech.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    @ExceptionHandler(ReferentielNotFoundException.class)
    public ProblemDetail handleReferentielNotFound(ReferentielNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Référentiel", ex.getMessage());
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ProblemDetail handleNoSuchElement(java.util.NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ProblemDetail handleAuth(Exception ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentification", "Identifiants invalides");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'avez pas les droits requis");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, "Accès refusé", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    @ExceptionHandler(EliminatoryWarningsConfirmationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleEliminatoryWarningsConfirmation(
            EliminatoryWarningsConfirmationRequiredException ex
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requiresConfirmation", true);
        body.put("nbCriteresEliminatoires", ex.getCount());
        body.put("message", ex.getMessage());
        body.put("details", ex.getDetails());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        return build(HttpStatus.BAD_REQUEST, "Règle métier violée", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    @ExceptionHandler(tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException.class)
    public ProblemDetail handleCritereEvalNotFound(tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    @ExceptionHandler(tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException.class)
    public ProblemDetail handleCritereEvalBadRequest(tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Validation", "Champs invalides");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", ex.getMessage());
    }

    private ProblemDetail build(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
