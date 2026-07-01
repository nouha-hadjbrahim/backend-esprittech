package tn.esprit.backend_esprittech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tn.esprit.backend_esprittech.dto.request.CreateUserRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateUserRequest;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetInvalidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.GlobalExceptionHandler;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.ReferentielService;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ReferentielService referentielService;

    @Mock
    private SujetProjetService sujetProjetService;

    @InjectMocks
    private AdminController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private SujetProjetResponse sampleSujetResponse() {
        return new SujetProjetResponse(
                1L, "Sujet test", CategorieSujet.PFE, "Description", "Objectifs",
                List.of("Soft skills"), List.of("Info"), List.of("Java"), 2,
                StatutSujet.SOUMIS_EN_VALIDATION, null, false, false,
                10L, "Sami Ben Ali", "sami@esprit.tn", null,
                LocalDateTime.now(), LocalDateTime.now(), null, null, null, null, false, 0, 0);
    }

    private Map<String, Object> validSujetPayload() {
        return Map.of(
                "titre", "Sujet test", "categorie", "PFE", "description", "Description",
                "objectifs", "Objectifs", "prerequis", List.of("Soft skills"),
                "domaines", List.of("Informatique"), "technologies", List.of("Java"), "capaciteAccueil", 2
        );
    }

    // ---------------- IMPORT REFERENTIEL ----------------

    @Test
    void importerReferentiel_validFile_returnsCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                "a,b,c".getBytes());

        when(referentielService.importerCsv(any())).thenReturn(5);

        mockMvc.perform(multipart("/api/admin/import-referentiel")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("5 utilisateur(s) importé(s) dans le référentiel"));

        verify(referentielService).importerCsv(any());
    }

    @Test
    void importerReferentiel_serviceThrows_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                "a,b,c".getBytes());

        when(referentielService.importerCsv(any()))
                .thenThrow(new RuntimeException("Parse error"));

        mockMvc.perform(multipart("/api/admin/import-referentiel")
                        .file(file))
                .andExpect(status().isInternalServerError());
    }

    // ---------------- CREATE USER ----------------

    @Test
    void createUser_validRequest_returnsCreated() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "Raed", "Bettaher", "raed@esprit.tn",
                "raed123", "Password1", Role.ROLE_ADMIN, true);

        User user = new User();
        UserResponse response = mock(UserResponse.class);

        when(userService.create(any())).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(userService).create(any());
        verify(userMapper).toResponse(user);
    }

    @Test
    void createUser_serviceThrows_returns500() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "Raed", "Bettaher", "raed@esprit.tn",
                "raed123", "Password1", Role.ROLE_ADMIN, true);

        when(userService.create(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ---------------- LIST USERS ----------------

    @Test
    void listerUsers_withoutSearch_returnsPage() throws Exception {
        Page<User> page = new PageImpl<>(List.of(new User()));

        when(userService.list(isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());

        verify(userService).list(isNull(), any(Pageable.class));
    }

    @Test
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

    @Test
    void listerUsers_emptyPage_returnsOk() throws Exception {
        Page<User> emptyPage = new PageImpl<>(List.of());

        when(userService.list(isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());

        verify(userService).list(isNull(), any(Pageable.class));
    }

    // ---------------- UPDATE USER ----------------

    @Test
    void updateUser_validRequest_returnsOk() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "Raed", "Bettaher", "raed@test.com",
                Role.ROLE_ADMIN, true);

        User user = new User();
        UserResponse response = mock(UserResponse.class);

        when(userService.update(eq(1L), any())).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).update(eq(1L), any());
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "Raed", "Bettaher", "raed@test.com",
                Role.ROLE_ADMIN, true);

        when(userService.update(eq(99L), any()))
                .thenThrow(new tn.esprit.backend_esprittech.exception.ResourceNotFoundException("User introuvable"));

        mockMvc.perform(put("/api/admin/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(userService).update(eq(99L), any());
    }

    // ---------------- DELETE USER ----------------

    @Test
    void deleteUser_validId_returnsOk() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur supprimé"));

        verify(userService).delete(1L);
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        doThrow(new tn.esprit.backend_esprittech.exception.ResourceNotFoundException("User introuvable"))
                .when(userService).delete(99L);

        mockMvc.perform(delete("/api/admin/users/99"))
                .andExpect(status().isNotFound());

        verify(userService).delete(99L);
    }

    // ---------------- GESTION SUJETS ----------------

    @Test
    void listerSujets_returnsPagedResponse() throws Exception {
        Page<SujetProjetResponse> page = new PageImpl<>(List.of(sampleSujetResponse()));
        when(sujetProjetService.listSujetsForAdmin(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/sujet-projets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(sujetProjetService).listSujetsForAdmin(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listerSujets_withFilters_delegatesToService() throws Exception {
        Page<SujetProjetResponse> page = new PageImpl<>(List.of(sampleSujetResponse()));
        when(sujetProjetService.listSujetsForAdmin(
                eq(CategorieSujet.PFE), eq(StatutSujet.SOUMIS_EN_VALIDATION), eq("java"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/sujet-projets")
                        .param("categorie", "PFE")
                        .param("statut", "SOUMIS_EN_VALIDATION")
                        .param("search", "java")
                        .param("page", "0")
                        .param("size", "8"))
                .andExpect(status().isOk());

        verify(sujetProjetService).listSujetsForAdmin(
                eq(CategorieSujet.PFE), eq(StatutSujet.SOUMIS_EN_VALIDATION), eq("java"), any(Pageable.class));
    }

    @Test
    void listerDemandes_returnsPagedResponse() throws Exception {
        Page<SujetProjetResponse> page = new PageImpl<>(List.of(sampleSujetResponse()));
        when(sujetProjetService.listDemandesForAdmin(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/sujet-projets/demandes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(sujetProjetService).listDemandesForAdmin(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void listerSujetsDisponibles_returnsPagedResponse() throws Exception {
        Page<SujetProjetResponse> page = new PageImpl<>(List.of(sampleSujetResponse()));
        when(sujetProjetService.listSujetsDisponiblesForAdmin(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/sujet-projets/disponibles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(sujetProjetService).listSujetsDisponiblesForAdmin(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getSujet_validId_returnsOk() throws Exception {
        when(sujetProjetService.getSujetForAdmin(1L)).thenReturn(sampleSujetResponse());

        mockMvc.perform(get("/api/admin/sujet-projets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titre").value("Sujet test"));

        verify(sujetProjetService).getSujetForAdmin(1L);
    }

    @Test
    void validerSujet_validId_returnsOk() throws Exception {
        SujetProjetResponse validated = sampleSujetResponse();
        when(sujetProjetService.validerSujetAsAdmin(1L)).thenReturn(validated);

        mockMvc.perform(post("/api/admin/sujet-projets/1/valider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(sujetProjetService).validerSujetAsAdmin(1L);
    }

    @Test
    void invaliderSujet_validRequest_returnsOk() throws Exception {
        when(sujetProjetService.invaliderSujetAsAdmin(eq(1L), any(SujetInvalidationRequest.class)))
                .thenReturn(sampleSujetResponse());

        mockMvc.perform(post("/api/admin/sujet-projets/1/invalider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motif", "Motif de refus"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(sujetProjetService).invaliderSujetAsAdmin(eq(1L), any(SujetInvalidationRequest.class));
    }

    @Test
    void invaliderSujet_blankMotif_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/sujet-projets/1/invalider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("motif", ""))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sujetProjetService);
    }

    @Test
    void modifierSujet_validRequest_returnsOk() throws Exception {
        when(sujetProjetService.modifierSujetAsAdmin(eq(1L), any(SujetProjetRequest.class)))
                .thenReturn(sampleSujetResponse());

        mockMvc.perform(put("/api/admin/sujet-projets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSujetPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Sujet test"));

        verify(sujetProjetService).modifierSujetAsAdmin(eq(1L), any(SujetProjetRequest.class));
    }

    @Test
    void supprimerSujet_validId_returnsOk() throws Exception {
        doNothing().when(sujetProjetService).supprimerSujetAsAdmin(1L);

        mockMvc.perform(delete("/api/admin/sujet-projets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sujet supprimé"));

        verify(sujetProjetService).supprimerSujetAsAdmin(1L);
    }
}
