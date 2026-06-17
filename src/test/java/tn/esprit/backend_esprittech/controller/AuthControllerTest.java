package tn.esprit.backend_esprittech.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.backend_esprittech.dto.request.LoginRequest;
import tn.esprit.backend_esprittech.dto.request.RefreshTokenRequest;
import tn.esprit.backend_esprittech.dto.request.RegisterRequest;
import tn.esprit.backend_esprittech.dto.response.AuthResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.AuthService;
import tn.esprit.backend_esprittech.service.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private UserService userService;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthController authController;

    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse(1L, "Test", "User", "test@esprit.tn",
                "ETU-001", Role.ROLE_ETUDIANT, TypeUtilisateur.ETUDIANT, null, true);
        authResponse = AuthResponse.of("access-token", "refresh-token", userResponse);
    }

    @Test
    void register_returns201() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "ETU-001", "password123");
        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void login_returns200() {
        var request = new LoginRequest("test@esprit.tn", "password123");
        when(authService.login(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void refresh_returns200() {
        var request = new RefreshTokenRequest("refresh-token");
        when(authService.refresh("refresh-token")).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.refresh(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void me_returns200() {
        User user = User.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn").identifiant("ETU-001")
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true).build();

        when(userService.getCurrentUser()).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = authController.me();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userResponse, response.getBody());
    }
}
