package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.dto.request.LoginRequest;
import tn.esprit.backend_esprittech.dto.request.RegisterRequest;
import tn.esprit.backend_esprittech.dto.response.AuthResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailAlreadyExistsException;
import tn.esprit.backend_esprittech.exception.OrganizationMembershipException;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.service.EmailVerificationService.PendingData;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserService userService;
    @Mock private AzureGraphService azureGraphService;
    @Mock private EmailVerificationService emailVerificationService;

    @InjectMocks private AuthService authService;

    private User user;
    private UserResponse userResponse;
    private AzureGraphService.GraphMember member;
    private PendingData pendingData;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .nom("Test").prenom("User")
                .email("test@esprit.tn")
                .password("encoded")
                .role(Role.ROLE_ETUDIANT)
                .typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true)
                .build();

        userResponse = new UserResponse(1L, "Test", "User", "test@esprit.tn",
                Role.ROLE_ETUDIANT, TypeUtilisateur.ETUDIANT, null, true, null,
                false, null, null);

        member = new AzureGraphService.GraphMember(TypeUtilisateur.ETUDIANT, "GL");
        pendingData = new PendingData("test@esprit.tn", "Test", "User",
                "encoded", TypeUtilisateur.ETUDIANT, "GL");
    }

    // ---- startRegistration (étape 1) ----

    @Test
    void startRegistration_success_sendsVerification() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "Password123");

        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(false);
        when(azureGraphService.resolveMember("test@esprit.tn")).thenReturn(member);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");

        authService.startRegistration(request);

        verify(emailVerificationService).startVerification(argThat(data ->
                data.email().equals("test@esprit.tn")
                        && data.encodedPassword().equals("encoded")
                        && data.type() == TypeUtilisateur.ETUDIANT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void startRegistration_emailNotInOrganization_throws() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "Password123");
        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(false);
        when(azureGraphService.resolveMember("test@esprit.tn"))
                .thenThrow(new OrganizationMembershipException("absent"));

        assertThrows(OrganizationMembershipException.class, () -> authService.startRegistration(request));
        verify(emailVerificationService, never()).startVerification(any());
    }

    @Test
    void startRegistration_emailAlreadyExists_throws() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "Password123");
        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.startRegistration(request));
        verify(azureGraphService, never()).resolveMember(any());
        verify(emailVerificationService, never()).startVerification(any());
    }

    // ---- verifyEmailAndCreate (étape 2) ----

    @Test
    void verifyEmailAndCreate_success() {
        when(emailVerificationService.consumeVerifiedRegistration("test@esprit.tn", "123456"))
                .thenReturn(pendingData);
        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userService.toEnrichedResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.verifyEmailAndCreate("test@esprit.tn", "123456");

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(userResponse, response.user());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyEmailAndCreate_emailAlreadyExists_throws() {
        when(emailVerificationService.consumeVerifiedRegistration("test@esprit.tn", "123456"))
                .thenReturn(pendingData);
        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.verifyEmailAndCreate("test@esprit.tn", "123456"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerificationCode_delegatesToService() {
        authService.resendVerificationCode("test@esprit.tn");
        verify(emailVerificationService).resendCode("test@esprit.tn");
    }

    // ---- login ----

    @Test
    void login_success() {
        var request = new LoginRequest("test@esprit.tn", "password123");

        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userService.toEnrichedResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_badCredentials_throws() {
        var request = new LoginRequest("test@esprit.tn", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_userNotFound_throws() {
        var request = new LoginRequest("gone@esprit.tn", "password123");
        when(userRepository.findByEmail("gone@esprit.tn")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    // ---- refresh ----

    @Test
    void refresh_success() {
        when(tokenProvider.validateToken("valid-refresh")).thenReturn(true);
        when(tokenProvider.isRefreshToken("valid-refresh")).thenReturn(true);
        when(tokenProvider.getEmailFromToken("valid-refresh")).thenReturn("test@esprit.tn");
        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("new-access");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("new-refresh");
        when(userService.toEnrichedResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.refresh("valid-refresh");

        assertNotNull(response);
        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
    }

    @Test
    void refresh_invalidToken_throws() {
        when(tokenProvider.validateToken("invalid")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh("invalid"));
    }

    @Test
    void refresh_notRefreshToken_throws() {
        when(tokenProvider.validateToken("access-tok")).thenReturn(true);
        when(tokenProvider.isRefreshToken("access-tok")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh("access-tok"));
    }

    @Test
    void refresh_userNotFound_throws() {
        when(tokenProvider.validateToken("valid")).thenReturn(true);
        when(tokenProvider.isRefreshToken("valid")).thenReturn(true);
        when(tokenProvider.getEmailFromToken("valid")).thenReturn("gone@esprit.tn");
        when(userRepository.findByEmail("gone@esprit.tn")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.refresh("valid"));
    }
}
