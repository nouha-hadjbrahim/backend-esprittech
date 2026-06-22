package tn.esprit.backend_esprittech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import tn.esprit.backend_esprittech.config.CustomUserDetailsService;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.config.SecurityConfig;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.CreateUserRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateUserRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;
import tn.esprit.backend_esprittech.service.ReferentielService;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.enums.Role;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock JWT infrastructure so Security filter chain can be satisfied
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private ReferentielService referentielService;

    @MockBean
    private EquipeRechercheService equipeService;

    @MockBean
    private EquipeMapper equipeMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    // ---------------- IMPORT REFERENTIEL ----------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void importerReferentiel_validFile_returnsCreated() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                "a,b,c".getBytes());

        when(referentielService.importerCsv(any())).thenReturn(5);

        mockMvc.perform(multipart("/api/admin/import-referentiel")
                .file(file)
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("5 utilisateur(s) importé(s) dans le référentiel"));

        verify(referentielService).importerCsv(any());
    }

    // ---------------- CREATE EQUIPE ----------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void creerEquipe_validRequest_returnsCreated() throws Exception {

        CreateEquipeRechercheRequest request = new CreateEquipeRechercheRequest("AI", "Desc", null, null);

        EquipeResponse response = mock(EquipeResponse.class);

        when(equipeService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/equipes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(equipeService).create(any());
    }

    // ---------------- ASSIGN CHEF ----------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignerChef_validRequest_returnsOk() throws Exception {

        EquipeRecherche equipe = new EquipeRecherche();

        when(equipeService.assignerChef(1L,2L)).thenReturn(equipe);
        when(equipeService.compterMembres(equipe)).thenReturn(10);
        when(equipeMapper.toResponse(equipe, 10))
                .thenReturn(mock(EquipeResponse.class));

        String body = """
                {
                    "chefId": 2
                }
                """;

        mockMvc.perform(put("/api/admin/equipes/1/chef")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        verify(equipeService).assignerChef(1L, 2L);
        verify(equipeService).compterMembres(equipe);
        verify(equipeMapper).toResponse(equipe, 10);
    }

    // ---------------- CREATE USER ----------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_validRequest_returnsCreated() throws Exception {

        CreateUserRequest request = new CreateUserRequest(
                "Raed",
                "Bettaher",
                "raed@esprit.tn",
                "raed123",
                "Password1",
                Role.ROLE_ADMIN,
                true
        );

        User user = new User();
        UserResponse response = mock(UserResponse.class);

        when(userService.create(any())).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(userService).create(any());
        verify(userMapper).toResponse(user);
    }

    // ---------------- LIST USERS ----------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void listerUsers_withoutSearch_returnsPage() throws Exception {

        Page<User> page = new PageImpl<>(List.of(new User()));

        when(userService.list(isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());

        verify(userService).list(isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listerUsers_withSearch_returnsPage() throws Exception {

        Page<User> page = new PageImpl<>(List.of(new User()));

        when(userService.list(eq("raed"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                .param("search", "raed")
                .param("page", "1")
                .param("size", "5"))
                .andExpect(status().isOk());

        verify(userService).list(eq("raed"), any(Pageable.class));
    }

    // ---------------- UPDATE USER ----------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_validRequest_returnsOk() throws Exception {

        UpdateUserRequest request = new UpdateUserRequest(
                "Raed",
                "Bettaher",
                "raed@test.com",
                Role.ROLE_ADMIN,
                true
        );

        User user = new User();
        UserResponse response = mock(UserResponse.class);

        when(userService.update(eq(1L), any())).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(put("/api/admin/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).update(eq(1L), any());
        verify(userMapper).toResponse(user);
    }

    // ---------------- DELETE USER ----------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_validId_returnsOk() throws Exception {

        mockMvc.perform(delete("/api/admin/users/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Utilisateur supprimé"));

        verify(userService).delete(1L);
    }

    // ---------------- SECURITY TEST ----------------

    @Test
    @WithMockUser(roles = "USER")
    void createUser_nonAdmin_returnsForbidden() throws Exception {

        CreateUserRequest request = new CreateUserRequest(
                "Raed",
                "Bettaher",
                "raed@esprit.tn",
                "raed123",
                "Password1",
                Role.ROLE_ADMIN,
                true
        );

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}