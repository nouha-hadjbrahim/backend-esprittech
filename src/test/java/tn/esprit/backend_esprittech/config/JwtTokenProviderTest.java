package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-that-is-at-least-32-bytes-long!");
        properties.setExpirationMs(900000);
        tokenProvider = new JwtTokenProvider(properties);

        user = User.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn")
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true).build();
    }

    @Test
    void generateAccessToken_isValid() {
        String token = tokenProvider.generateAccessToken(user);
        assertTrue(tokenProvider.validateToken(token));
        assertFalse(tokenProvider.isRefreshToken(token));
        assertEquals("test@esprit.tn", tokenProvider.getEmailFromToken(token));
    }

    @Test
    void generateRefreshToken_isRefresh() {
        String token = tokenProvider.generateRefreshToken(user);
        assertTrue(tokenProvider.validateToken(token));
        assertTrue(tokenProvider.isRefreshToken(token));
        assertEquals("test@esprit.tn", tokenProvider.getEmailFromToken(token));
    }

    @Test
    void validateToken_expired_returnsFalse() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-that-is-at-least-32-bytes-long!");
        properties.setExpirationMs(-1000);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(properties);

        String token = expiredProvider.generateAccessToken(user);
        assertFalse(expiredProvider.validateToken(token));
    }

    @Test
    void validateToken_garbage_returnsFalse() {
        assertFalse(tokenProvider.validateToken("this.is.not.a.jwt"));
    }

    @Test
    void validateToken_null_returnsFalse() {
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void constructor_nullSecret_throws() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(null);
        properties.setExpirationMs(900000);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(properties));

        assertTrue(exception.getMessage().contains("JWT_SECRET is missing or empty"));
        assertTrue(exception.getMessage().contains(".env.example"));
    }

    @Test
    void constructor_shortSecret_throws() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("short");
        properties.setExpirationMs(900000);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(properties));

        assertTrue(exception.getMessage().contains("JWT_SECRET is too short for HS256"));
        assertTrue(exception.getMessage().contains("at least 32 UTF-8 bytes"));
    }
}
