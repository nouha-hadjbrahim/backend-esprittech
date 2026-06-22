package tn.esprit.backend_esprittech.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EquipeMapperTest {

    private UserMapper userMapper;
    private EquipeMapper mapper;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        mapper = new EquipeMapper(userMapper);
    }

    // ---------------- toEntity ----------------

    @Test
    void toEntity_null_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_valid_mapsFields() {
        CreateEquipeRechercheRequest request = mock(CreateEquipeRechercheRequest.class);
        when(request.nom()).thenReturn("AI Team");
        when(request.description()).thenReturn("Research");

        EquipeRecherche result = mapper.toEntity(request);

        assertNotNull(result);
        assertEquals("AI Team", result.getNom());
        assertEquals("Research", result.getDescription());
    }

    // ---------------- updateEntity ----------------

    @Test
    void updateEntity_nullRequest_noChange() {
        EquipeRecherche equipe = new EquipeRecherche();
        mapper.updateEntity(null, equipe);

        assertNull(equipe.getNom());
    }

    @Test
    void updateEntity_nullEntity_noChange() {
        UpdateEquipeRechercheRequest request = mock(UpdateEquipeRechercheRequest.class);
        assertDoesNotThrow(() -> mapper.updateEntity(request, null));
    }

    @Test
    void updateEntity_bothNullFields_noChange() {
        UpdateEquipeRechercheRequest request = mock(UpdateEquipeRechercheRequest.class);

        when(request.nom()).thenReturn(null);
        when(request.description()).thenReturn(null);

        EquipeRecherche equipe = new EquipeRecherche();
        equipe.setNom("Old");
        equipe.setDescription("OldDesc");

        mapper.updateEntity(request, equipe);

        assertEquals("Old", equipe.getNom());
        assertEquals("OldDesc", equipe.getDescription());
    }

    @Test
    void updateEntity_nomOnly_updatesNom() {
        UpdateEquipeRechercheRequest request = mock(UpdateEquipeRechercheRequest.class);
        when(request.nom()).thenReturn("NewName");
        when(request.description()).thenReturn(null);

        EquipeRecherche equipe = new EquipeRecherche();
        equipe.setNom("Old");

        mapper.updateEntity(request, equipe);

        assertEquals("NewName", equipe.getNom());
    }

    @Test
    void updateEntity_descriptionOnly_updatesDescription() {
        UpdateEquipeRechercheRequest request = mock(UpdateEquipeRechercheRequest.class);
        when(request.nom()).thenReturn(null);
        when(request.description()).thenReturn("NewDesc");

        EquipeRecherche equipe = new EquipeRecherche();
        equipe.setDescription("OldDesc");

        mapper.updateEntity(request, equipe);

        assertEquals("NewDesc", equipe.getDescription());
    }

    @Test
    void updateEntity_fullUpdate_updatesBoth() {
        UpdateEquipeRechercheRequest request = mock(UpdateEquipeRechercheRequest.class);
        when(request.nom()).thenReturn("AI");
        when(request.description()).thenReturn("Research");

        EquipeRecherche equipe = new EquipeRecherche();

        mapper.updateEntity(request, equipe);

        assertEquals("AI", equipe.getNom());
        assertEquals("Research", equipe.getDescription());
    }

    // ---------------- toResponse ----------------

    @Test
    void toResponse_null_returnsNull() {
        assertNull(mapper.toResponse(null, 5));
    }

    @Test
    void toResponse_valid_returnsDto() {
        EquipeRecherche equipe = new EquipeRecherche();
        equipe.setId(1L);
        equipe.setNom("AI");
        equipe.setDescription("Research");
        equipe.setCreatedAt(Instant.now());

        User chef = new User();
        equipe.setChef(chef);

        UserResponse chefResponse = mock(UserResponse.class);
        UserResponse memberResponse = mock(UserResponse.class);

        equipe.setMembers(List.of(new User()));

        when(userMapper.toResponse(chef)).thenReturn(chefResponse);
        when(userMapper.toResponse(any(User.class))).thenReturn(memberResponse);

        EquipeResponse response = mapper.toResponse(equipe, 3);

        assertNotNull(response);
        assertEquals("AI", response.nom());
        assertEquals("Research", response.description());

        verify(userMapper).toResponse(chef);
    }
}