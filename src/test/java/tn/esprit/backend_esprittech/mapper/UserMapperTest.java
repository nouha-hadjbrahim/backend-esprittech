package tn.esprit.backend_esprittech.mapper;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toResponse_mapsAllFields() {
        User user = User.builder()
                .id(1L)
                .nom("Hadjbrahim").prenom("Nouha")
                .email("nouha@esprit.tn")
                .role(Role.ROLE_ETUDIANT)
                .typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .departement("INFO")
                .enabled(true)
                .build();

        UserResponse response = mapper.toResponse(user);

        assertEquals(1L, response.id());
        assertEquals("Hadjbrahim", response.nom());
        assertEquals("Nouha", response.prenom());
        assertEquals("nouha@esprit.tn", response.email());
        assertEquals(Role.ROLE_ETUDIANT, response.role());
        assertEquals(TypeUtilisateur.ETUDIANT, response.typeUtilisateur());
        assertEquals("INFO", response.departement());
        assertTrue(response.enabled());
    }

    @Test
    void toResponse_nullUser_returnsNull() {
        assertNull(mapper.toResponse(null));
    }
}
