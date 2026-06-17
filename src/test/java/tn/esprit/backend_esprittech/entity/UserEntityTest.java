package tn.esprit.backend_esprittech.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    private User buildUser() {
        return User.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn").identifiant("ETU-001")
                .password("encoded")
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .departement("INFO")
                .enabled(true).build();
    }

    @Test
    void getUsername_returnsEmail() {
        User user = buildUser();
        assertEquals("test@esprit.tn", user.getUsername());
    }

    @Test
    void getAuthorities_returnsRole() {
        User user = buildUser();
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ETUDIANT", authorities.iterator().next().getAuthority());
    }

    @Test
    void accountStatus_allTrue() {
        User user = buildUser();
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void isEnabled_whenDisabled_returnsFalse() {
        User user = buildUser();
        user.setEnabled(false);
        assertFalse(user.isEnabled());
    }

    @Test
    void onCreate_setsCreatedAt() {
        User user = buildUser();
        assertNull(user.getCreatedAt());
        user.onCreate();
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void onCreate_doesNotOverrideExistingCreatedAt() {
        User user = buildUser();
        user.onCreate();
        var first = user.getCreatedAt();
        user.onCreate();
        assertEquals(first, user.getCreatedAt());
    }

    @Test
    void builder_defaultEnabled() {
        User user = User.builder()
                .nom("A").prenom("B").email("a@b.tn").identifiant("X")
                .password("p").role(Role.ROLE_ADMIN).typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                .build();
        assertTrue(user.isEnabled());
    }

    @Test
    void noArgsConstructor_works() {
        User user = new User();
        assertNull(user.getId());
        assertNull(user.getEmail());
    }
}
