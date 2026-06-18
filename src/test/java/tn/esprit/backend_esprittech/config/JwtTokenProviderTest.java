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
        DotenvConfig config = new DotenvConfig();
        config.setSecret("unit-test-secret-key-that-is-at-least-32-bytes-long!");
        config.setAccessExpiration(900000);
        config.setRefreshExpiration(604800000);
        tokenProvider = new JwtTokenProvider(config);

        user = User.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn").identifiant("ETU-001")
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
        DotenvConfig config = new DotenvConfig();
        config.setSecret("unit-test-secret-key-that-is-at-least-32-bytes-long!");
        config.setAccessExpiration(-1000);
        config.setRefreshExpiration(604800000);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(config);

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
        DotenvConfig config = new DotenvConfig();
        config.setSecret(null);
        config.setAccessExpiration(900000);
        config.setRefreshExpiration(604800000);

        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(config));
    }

    @Test
    void constructor_shortSecret_throws() {
        DotenvConfig config = new DotenvConfig();
        config.setSecret("short");
        config.setAccessExpiration(900000);
        config.setRefreshExpiration(604800000);

        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(config));
    }
}
