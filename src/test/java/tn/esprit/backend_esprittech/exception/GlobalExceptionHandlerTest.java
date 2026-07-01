package tn.esprit.backend_esprittech.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailExists_returns409() {
        ProblemDetail pd = handler.handleEmailExists(
                new EmailAlreadyExistsException("email déjà utilisé"));

        assertEquals(409, pd.getStatus());
        assertEquals("Conflit", pd.getTitle());
        assertEquals("email déjà utilisé", pd.getDetail());
        assertNotNull(pd.getProperties().get("timestamp"));
    }

    @Test
    void handleAuth_returns401() {
        ProblemDetail pd = handler.handleAuth(new BadCredentialsException("bad"));

        assertEquals(401, pd.getStatus());
        assertEquals("Authentification", pd.getTitle());
        assertEquals("Identifiants invalides", pd.getDetail());
    }

    @Test
    void handleBusinessConflict_returns409WithMessageProperty() {
        ProblemDetail pd = handler.handleConflict(new BusinessConflictException("Conflit metier"));

        assertEquals(409, pd.getStatus());
        assertEquals("Conflit", pd.getTitle());
        assertEquals("Conflit metier", pd.getDetail());
        assertEquals("Conflit metier", pd.getProperties().get("message"));
    }

    @Test
    void handleDataIntegrityViolation_returns409() {
        ProblemDetail pd = handler.handleDataIntegrityViolation(
                new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        assertEquals(409, pd.getStatus());
        assertEquals("Conflit", pd.getTitle());
        assertEquals("Contrainte d'unicite ou d'integrite violee.", pd.getDetail());
    }

    @Test
    void handleAccessDenied_returns403() {
        ProblemDetail pd = handler.handleAccessDenied(new AccessDeniedException("nope"));

        assertEquals(403, pd.getStatus());
        assertEquals("Accès refusé", pd.getTitle());
    }

    @Test
    void handleIllegalArgument_returns400() {
        ProblemDetail pd = handler.handleIllegalArgument(new IllegalArgumentException("bad arg"));

        assertEquals(400, pd.getStatus());
        assertEquals("Requête invalide", pd.getTitle());
        assertEquals("bad arg", pd.getDetail());
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "email", "Format invalide");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail pd = handler.handleValidation(ex);

        assertEquals(400, pd.getStatus());
        assertEquals("Validation", pd.getTitle());
        assertEquals("Format invalide", pd.getDetail());
        assertEquals("Format invalide", pd.getProperties().get("message"));
        assertNotNull(pd.getProperties().get("errors"));
    }

    @Test
    void handleGeneric_returns500() {
        ProblemDetail pd = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(500, pd.getStatus());
        assertEquals("Erreur interne", pd.getTitle());
        assertEquals("boom", pd.getDetail());
    }

    @Test
    void handleBusiness_returns400() {
        BusinessException ex = new BusinessException("business error");

        ProblemDetail pd = handler.handleBusiness(ex);

        assertEquals(400, pd.getStatus());
        assertEquals("Règle métier violée", pd.getTitle());
        assertEquals("business error", pd.getDetail());
    }

    @Test
    void handleNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found error");

        ProblemDetail pd = handler.handleNotFound(ex);

        assertEquals(404, pd.getStatus());
        assertEquals("Ressource introuvable", pd.getTitle());
        assertEquals("not found error", pd.getDetail());
    }
    @Test
    void handleReferentielNotFound_returnsProblemDetail() {
        ReferentielNotFoundException ex =
                new ReferentielNotFoundException("Référentiel introuvable");

        ProblemDetail result = handler.handleReferentielNotFound(ex);

        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("Référentiel", result.getTitle());
        assertEquals("Référentiel introuvable", result.getDetail());
    }

    @Test
    void handleNoSuchElement_returnsProblemDetail() {
        NoSuchElementException ex =
                new NoSuchElementException("Utilisateur non trouvé");

        ProblemDetail result = handler.handleNoSuchElement(ex);

        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("Ressource introuvable", result.getTitle());
        assertEquals("Utilisateur non trouvé", result.getDetail());
    }

    @Test
    void handleCritereEvalNotFound_returns404() {
        tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException ex =
                new tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException("Critère non trouvé");

        ProblemDetail pd = handler.handleCritereEvalNotFound(ex);

        assertEquals(404, pd.getStatus());
        assertEquals("Ressource introuvable", pd.getTitle());
        assertEquals("Critère non trouvé", pd.getDetail());
        assertNotNull(pd.getProperties().get("timestamp"));
    }

    @Test
    void handleCritereEvalBadRequest_returns400() {
        tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException ex =
                new tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException("Requête invalide");

        ProblemDetail pd = handler.handleCritereEvalBadRequest(ex);

        assertEquals(400, pd.getStatus());
        assertEquals("Requête invalide", pd.getTitle());
        assertEquals("Requête invalide", pd.getDetail());
        assertNotNull(pd.getProperties().get("timestamp"));
    }

    @Test
    void handleValidation_multipleFieldErrors_returnsAllErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("request", "email", "Format invalide");
        FieldError error2 = new FieldError("request", "password", "Mot de passe faible");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        ProblemDetail pd = handler.handleValidation(ex);

        assertEquals(400, pd.getStatus());
        assertEquals("Validation", pd.getTitle());
        assertNotNull(pd.getProperties().get("errors"));
    }

    @Test
    void handleEmailExists_propertyHasTimestamp() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("email exists");
        ProblemDetail pd = handler.handleEmailExists(ex);

        assertNotNull(pd.getProperties().get("timestamp"));
        assertTrue(pd.getProperties().get("timestamp") instanceof java.time.Instant);
    }

    @Test
    void handleGeneric_differentException_returns500() {
        Exception ex = new Exception("Database connection error");
        ProblemDetail pd = handler.handleGeneric(ex);

        assertEquals(500, pd.getStatus());
        assertEquals("Erreur interne", pd.getTitle());
        assertEquals("Database connection error", pd.getDetail());
    }

    @Test
    void handleBusiness_differentMessage_returns400() {
        BusinessException ex = new BusinessException("Solde insuffisant");
        ProblemDetail pd = handler.handleBusiness(ex);

        assertEquals(400, pd.getStatus());
        assertEquals("Règle métier violée", pd.getTitle());
        assertEquals("Solde insuffisant", pd.getDetail());
    }

    @Test
    void handleReferentielNotFound_differentMessage_returns404() {
        ReferentielNotFoundException ex = new ReferentielNotFoundException("Fichier not found");
        ProblemDetail pd = handler.handleReferentielNotFound(ex);

        assertEquals(404, pd.getStatus());
        assertEquals("Référentiel", pd.getTitle());
        assertEquals("Fichier not found", pd.getDetail());
    }

    @Test
    void handleAccessDenied_hasTimestamp() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ProblemDetail pd = handler.handleAccessDenied(ex);

        assertNotNull(pd.getProperties().get("timestamp"));
        assertTrue(pd.getProperties().get("timestamp") instanceof java.time.Instant);
    }

    @Test
    void handleIllegalArgument_hasTimestamp() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        ProblemDetail pd = handler.handleIllegalArgument(ex);

        assertNotNull(pd.getProperties().get("timestamp"));
    }

    @Test
    void handleNotFound_hasTimestamp() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ProblemDetail pd = handler.handleNotFound(ex);

        assertNotNull(pd.getProperties().get("timestamp"));
    }

    @Test
    void handleValidation_noFieldErrors_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ProblemDetail pd = handler.handleValidation(ex);

        assertEquals(400, pd.getStatus());
        assertEquals("Validation", pd.getTitle());
        assertNotNull(pd.getProperties().get("errors"));
    }
}
