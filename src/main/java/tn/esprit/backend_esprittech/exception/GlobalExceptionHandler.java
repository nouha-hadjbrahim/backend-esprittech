package tn.esprit.backend_esprittech.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    @ExceptionHandler(OrganizationMembershipException.class)
    public ProblemDetail handleOrganizationMembership(OrganizationMembershipException ex) {
        return build(HttpStatus.NOT_FOUND, "Organisation", ex.getMessage());
    }

    @ExceptionHandler(AzureGraphException.class)
    public ProblemDetail handleAzureGraph(AzureGraphException ex) {
        log.error("Erreur d'appel à Azure AD / Microsoft Graph", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible", ex.getMessage());
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ProblemDetail handleEmailVerification(EmailVerificationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Vérification email", ex.getMessage());
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

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'avez pas les droits requis");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, "Accès refusé", ex.getMessage());
    }

    @ExceptionHandler({ConflictException.class, BusinessConflictException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Violation d'intégrité de données", ex);
        return build(HttpStatus.CONFLICT, "Conflit", "Contrainte d'unicité ou d'intégrité violée.");
    }

    @ExceptionHandler(JpaSystemException.class)
    public ProblemDetail handleJpaSystemException(JpaSystemException ex) {
        log.error("Erreur JPA/SQL inattendue", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur technique est survenue. Veuillez réessayer plus tard.");
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
        String detail = errors.values().stream()
                .findFirst()
                .orElse("Champs invalides");
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Validation", detail);
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Erreur interne non gérée", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue est survenue. Veuillez réessayer plus tard.");
    }

    private ProblemDetail build(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("message", detail);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
