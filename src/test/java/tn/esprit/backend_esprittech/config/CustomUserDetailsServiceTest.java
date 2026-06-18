package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_found() {
        User user = User.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn").identifiant("ETU-001")
                .password("encoded")
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true).build();

        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("test@esprit.tn");

        assertEquals("test@esprit.tn", result.getUsername());
    }

    @Test
    void loadUserByUsername_notFound_throws() {
        when(userRepository.findByEmail("unknown@esprit.tn")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown@esprit.tn"));
    }
}
