package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.repository.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void run_adminNotExists_createsAdmin() {
        when(userRepository.existsByEmail("admin@test.tn")).thenReturn(false);
        when(passwordEncoder.encode("Admin@1234")).thenReturn("encoded");

        DataInitializer init = new DataInitializer(
                userRepository, passwordEncoder, "admin@test.tn", "Admin@1234");
        init.run();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void run_adminAlreadyExists_skips() {
        when(userRepository.existsByEmail("admin@test.tn")).thenReturn(true);

        DataInitializer init = new DataInitializer(
                userRepository, passwordEncoder, "admin@test.tn", "Admin@1234");
        init.run();

        verify(userRepository, never()).save(any());
    }
}
