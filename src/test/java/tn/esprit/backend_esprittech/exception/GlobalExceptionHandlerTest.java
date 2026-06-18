package tn.esprit.backend_esprittech.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

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
        assertNotNull(pd.getProperties().get("errors"));
    }

    @Test
    void handleGeneric_returns500() {
        ProblemDetail pd = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(500, pd.getStatus());
        assertEquals("Erreur interne", pd.getTitle());
        assertEquals("boom", pd.getDetail());
    }
}
