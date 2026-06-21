package tn.esprit.backend_esprittech.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.dto.request.AssignChefRequest;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRequest;
import tn.esprit.backend_esprittech.dto.request.CreateUserRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateUserRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.MessageResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;
import tn.esprit.backend_esprittech.service.ReferentielService;
import tn.esprit.backend_esprittech.service.UserService;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private ReferentielService referentielService;
    @Mock private EquipeRechercheService equipeService;
    @Mock private EquipeMapper equipeMapper;
    @Mock private UserService userService;
    @Mock private UserMapper userMapper;

    @InjectMocks private AdminController adminController;

    private User user;
    private UserResponse userResponse;
    private EquipeResponse equipeResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).nom("Test").prenom("User")
                .email("test@esprit.tn").identifiant("ETU-001")
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true).build();
        userResponse = new UserResponse(1L, "Test", "User", "test@esprit.tn",
                "ETU-001", Role.ROLE_ETUDIANT, TypeUtilisateur.ETUDIANT, null, true, null);
        equipeResponse = new EquipeResponse(1L, "AI Lab", "desc", null, 1, null);
    }

    @Test
    void importerReferentiel_returns201WithCount() {
        MultipartFile file = new MockMultipartFile("file", "ref.csv", "text/csv",
                "data".getBytes(StandardCharsets.UTF_8));
        when(referentielService.importerCsv(file)).thenReturn(5);

        ResponseEntity<MessageResponse> resp = adminController.importerReferentiel(file);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().message().contains("5"));
    }

    @Test
    void creerEquipe_returns201() {
        var request = new CreateEquipeRequest("AI Lab", "desc", 5L);
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("AI Lab").build();
        when(equipeService.creerEquipe(request)).thenReturn(equipe);
        when(equipeService.compterMembres(equipe)).thenReturn(1);
        when(equipeMapper.toResponse(equipe, 1)).thenReturn(equipeResponse);

        ResponseEntity<EquipeResponse> resp = adminController.creerEquipe(request);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(equipeResponse, resp.getBody());
    }

    @Test
    void assignerChef_returns200() {
        var request = new AssignChefRequest(5L);
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("AI Lab").build();
        when(equipeService.assignerChef(1L, 5L)).thenReturn(equipe);
        when(equipeService.compterMembres(equipe)).thenReturn(1);
        when(equipeMapper.toResponse(equipe, 1)).thenReturn(equipeResponse);

        ResponseEntity<EquipeResponse> resp = adminController.assignerChef(1L, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(equipeResponse, resp.getBody());
    }

    @Test
    void createUser_returns201() {
        var request = new CreateUserRequest("Test", "User", "test@esprit.tn",
                "ETU-001", "Passw0rd1", Role.ROLE_ETUDIANT, true);
        when(userService.create(request)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        ResponseEntity<UserResponse> resp = adminController.createUser(request);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(userResponse, resp.getBody());
    }

    @Test
    void listerUsers_returns200WithPagedBody() {
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        when(userService.list(any(), any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        ResponseEntity<PagedResponse<UserResponse>> resp = adminController.listerUsers(0, 10, null);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().totalElements());
        assertEquals(userResponse, resp.getBody().content().get(0));
    }

    @Test
    void updateUser_returns200() {
        var request = new UpdateUserRequest("Test", "User", "test@esprit.tn", Role.ROLE_ETUDIANT, true);
        when(userService.update(1L, request)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        ResponseEntity<UserResponse> resp = adminController.updateUser(1L, request);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(userResponse, resp.getBody());
    }

    @Test
    void deleteUser_returns200AndDelegates() {
        ResponseEntity<MessageResponse> resp = adminController.deleteUser(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(userService).delete(1L);
    }
}
