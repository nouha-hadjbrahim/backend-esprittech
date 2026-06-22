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
import tn.esprit.backend_esprittech.entity.ReferentielUtilisateur;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailAlreadyExistsException;
import tn.esprit.backend_esprittech.exception.ReferentielNotFoundException;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;
    @Mock private ReferentielService referentielService;

    @InjectMocks private AuthService authService;

    private User user;
    private UserResponse userResponse;
    private ReferentielUtilisateur reference;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .nom("Test").prenom("User")
                .email("test@esprit.tn")
                .identifiant("ETU-001")
                .password("encoded")
                .role(Role.ROLE_ETUDIANT)
                .typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true)
                .build();

        userResponse = new UserResponse(1L, "Test", "User", "test@esprit.tn",
                "ETU-001", Role.ROLE_ETUDIANT, TypeUtilisateur.ETUDIANT, null, true, null);

        reference = ReferentielUtilisateur.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn").identifiant("ETU-001")
                .type(TypeUtilisateur.ETUDIANT)
                .build();
    }

    // ---- register ----

    @Test
    void register_success() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "ETU-001", "Password123");

        when(referentielService.trouverCorrespondance("test@esprit.tn", "ETU-001")).thenReturn(reference);
        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(false);
        when(userRepository.existsByIdentifiant("ETU-001")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(userResponse, response.user());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_notInReferentiel_throws() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "ETU-001", "Password123");
        when(referentielService.trouverCorrespondance("test@esprit.tn", "ETU-001"))
                .thenThrow(new ReferentielNotFoundException("absent"));

        assertThrows(ReferentielNotFoundException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailAlreadyExists_throws() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "ETU-001", "Password123");
        when(referentielService.trouverCorrespondance("test@esprit.tn", "ETU-001")).thenReturn(reference);
        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_identifiantAlreadyExists_throws() {
        var request = new RegisterRequest("Test", "User", "test@esprit.tn", "ETU-001", "Password123");
        when(referentielService.trouverCorrespondance("test@esprit.tn", "ETU-001")).thenReturn(reference);
        when(userRepository.existsByEmail("test@esprit.tn")).thenReturn(false);
        when(userRepository.existsByIdentifiant("ETU-001")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    // ---- login ----

    @Test
    void login_success() {
        var request = new LoginRequest("test@esprit.tn", "password123");

        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userMapper.toResponse(user)).thenReturn(userResponse);

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
        when(userMapper.toResponse(user)).thenReturn(userResponse);

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
