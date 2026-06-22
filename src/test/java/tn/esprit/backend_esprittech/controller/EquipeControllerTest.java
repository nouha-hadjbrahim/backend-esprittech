package tn.esprit.backend_esprittech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EquipeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EquipeRechercheService equipeService;

    @Mock
    private EquipeMapper equipeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private EquipeController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------------- GET ALL ----------------

    @Test
    void listerEquipes_returns200() throws Exception {

        EquipeRecherche e1 = new EquipeRecherche();
        EquipeRecherche e2 = new EquipeRecherche();

        when(equipeService.listerToutes()).thenReturn(List.of(e1, e2));
        when(equipeService.compterMembres(any())).thenReturn(2);

        when(equipeMapper.toResponse(any(), eq(2)))
                .thenReturn(mock(EquipeResponse.class));

        mockMvc.perform(get("/api/equipes"))
                .andExpect(status().isOk());

        verify(equipeService).listerToutes();
    }

    // ---------------- GET BY ID ----------------

    @Test
    void getEquipe_returns200() throws Exception {

        EquipeRecherche equipe = new EquipeRecherche();

        when(equipeService.getEntite(1L)).thenReturn(equipe);
        when(equipeService.compterMembres(equipe)).thenReturn(3);
        when(equipeMapper.toResponse(equipe, 3))
                .thenReturn(mock(EquipeResponse.class));

        mockMvc.perform(get("/api/equipes/1"))
                .andExpect(status().isOk());

        verify(equipeService).getEntite(1L);
    }

    // ---------------- GET MEMBERS ----------------

    @Test
    void getMembres_returns200() throws Exception {

        User user = new User();

        when(equipeService.getMembres(1L)).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(mock(UserResponse.class));

        mockMvc.perform(get("/api/equipes/1/membres"))
                .andExpect(status().isOk());

        verify(equipeService).getMembres(1L);
    }

    // ---------------- CREATE ----------------

    @Test
    void create_returns201() throws Exception {

        CreateEquipeRechercheRequest request =
                new CreateEquipeRechercheRequest("AI", "Desc", null, null);

        EquipeResponse response = mock(EquipeResponse.class);

        when(equipeService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(equipeService).create(any());
    }

    // ---------------- UPDATE ----------------

    @Test
    void update_returns200() throws Exception {

        UpdateEquipeRechercheRequest request =
                new UpdateEquipeRechercheRequest("AI2", "Desc2", null, null);

        EquipeResponse response = mock(EquipeResponse.class);

        when(equipeService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/equipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(equipeService).update(eq(1L), any());
    }

    // ---------------- DELETE ----------------

    @Test
    void delete_returns204() throws Exception {

        doNothing().when(equipeService).delete(1L);

        mockMvc.perform(delete("/api/equipes/1"))
                .andExpect(status().isNoContent());

        verify(equipeService).delete(1L);
    }
}