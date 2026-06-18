package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    private User user;

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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- findById ----

    @Test
    void findById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertEquals(user, result);
    }

    @Test
    void findById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.findById(99L));
    }

    // ---- findByEmail ----

    @Test
    void findByEmail_success() {
        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));

        User result = userService.findByEmail("test@esprit.tn");

        assertEquals(user, result);
    }

    @Test
    void findByEmail_notFound_throws() {
        when(userRepository.findByEmail("unknown@esprit.tn")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.findByEmail("unknown@esprit.tn"));
    }

    // ---- getCurrentUser ----

    @Test
    void getCurrentUser_nullAuthentication_throws() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThrows(NoSuchElementException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_notAuthenticated_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NoSuchElementException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_stringPrincipal_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NoSuchElementException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_withUserPrincipal_returnsUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User result = userService.getCurrentUser();

        assertEquals(user, result);
    }

    @Test
    void getCurrentUser_otherPrincipal_fallsBackToFindByEmail() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(42);
        when(auth.getName()).thenReturn("test@esprit.tn");
        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User result = userService.getCurrentUser();

        assertEquals(user, result);
    }
}
