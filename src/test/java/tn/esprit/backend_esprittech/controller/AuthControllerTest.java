package tn.esprit.backend_esprittech.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tn.esprit.backend_esprittech.config.AuthCookieFactory;
import tn.esprit.backend_esprittech.dto.request.LoginRequest;
import tn.esprit.backend_esprittech.dto.request.RegisterRequest;
import tn.esprit.backend_esprittech.dto.response.AuthResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.service.AuthService;
import tn.esprit.backend_esprittech.service.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private UserService userService;
    @Mock private AuthCookieFactory cookieFactory;

    @InjectMocks private AuthController authController;

    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse(1L, "Test", "User", "test@esprit.tn",
                "ETU-001", Role.ROLE_ETUDIANT, TypeUtilisateur.ETUDIANT, null, true, null,
                false, null, null);
        authResponse = AuthResponse.of("access-token", "refresh-token", userResponse);
    }

    /** Stubs des cookies access + refresh, communs aux flux login/register/refresh. */
    private void stubAuthCookies() {
        when(cookieFactory.accessToken("access-token"))
                .thenReturn(ResponseCookie.from("access_token", "access-token").path("/api").build());
        when(cookieFactory.refreshToken("refresh-token"))
                .thenReturn(ResponseCookie.from("refresh_token", "refresh-token").path("/api/auth/refresh").build());
    }

    @Test
    void register_returns201_withUserBodyAndCookies() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "ETU-001", "password123");
        when(authService.register(request)).thenReturn(authResponse);
        stubAuthCookies();
        var response = new MockHttpServletResponse();

        ResponseEntity<UserResponse> result = authController.register(request, response);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(userResponse, result.getBody());
        assertEquals(2, response.getHeaders(HttpHeaders.SET_COOKIE).size());
    }

    @Test
    void login_returns200_withUserBodyAndCookies() {
        var request = new LoginRequest("test@esprit.tn", "password123");
        when(authService.login(request)).thenReturn(authResponse);
        stubAuthCookies();
        var response = new MockHttpServletResponse();

        ResponseEntity<UserResponse> result = authController.login(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userResponse, result.getBody());
        assertTrue(response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .anyMatch(h -> h.startsWith("access_token=")));
    }

    @Test
    void refresh_validCookie_returns200() {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "refresh-token"));
        when(authService.refresh("refresh-token")).thenReturn(authResponse);
        stubAuthCookies();
        var response = new MockHttpServletResponse();

        ResponseEntity<UserResponse> result = authController.refresh(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userResponse, result.getBody());
    }

    @Test
    void refresh_missingCookie_throwsUnauthorized() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        assertThrows(BadCredentialsException.class, () -> authController.refresh(request, response));
        verifyNoInteractions(authService);
    }

    @Test
    void logout_clearsBothCookies_returns204() {
        when(cookieFactory.clearAccessToken())
                .thenReturn(ResponseCookie.from("access_token", "").path("/api").maxAge(0).build());
        when(cookieFactory.clearRefreshToken())
                .thenReturn(ResponseCookie.from("refresh_token", "").path("/api/auth/refresh").maxAge(0).build());
        var response = new MockHttpServletResponse();

        ResponseEntity<Void> result = authController.logout(response);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertEquals(2, response.getHeaders(HttpHeaders.SET_COOKIE).size());
    }

    @Test
    void me_returns200() {
        when(userService.getCurrentUserResponse()).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = authController.me();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userResponse, response.getBody());
    }
}
