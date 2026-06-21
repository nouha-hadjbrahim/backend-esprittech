package tn.esprit.backend_esprittech.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipeControllerTest {

    @Mock private EquipeRechercheService equipeService;
    @Mock private EquipeMapper equipeMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks private EquipeController equipeController;

    private EquipeRecherche equipe;
    private EquipeResponse equipeResponse;
    private User membre;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        equipe = EquipeRecherche.builder().id(1L).nom("AI Lab").build();
        equipeResponse = new EquipeResponse(1L, "AI Lab", "desc", null, 1, null);
        membre = User.builder()
                .id(5L).nom("Prof").prenom("Test")
                .email("prof@esprit.tn").identifiant("EA-5")
                .role(Role.ROLE_CHEF_EQUIPE).typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                .enabled(true).build();
        userResponse = new UserResponse(5L, "Prof", "Test", "prof@esprit.tn",
                "EA-5", Role.ROLE_CHEF_EQUIPE, TypeUtilisateur.ENSEIGNANT, null, true, null);
    }

    @Test
    void listerEquipes_returns200WithList() {
        when(equipeService.listerToutes()).thenReturn(List.of(equipe));
        when(equipeService.compterMembres(equipe)).thenReturn(1);
        when(equipeMapper.toResponse(eq(equipe), anyInt())).thenReturn(equipeResponse);

        ResponseEntity<List<EquipeResponse>> resp = equipeController.listerEquipes();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals(equipeResponse, resp.getBody().get(0));
    }

    @Test
    void getEquipe_returns200() {
        when(equipeService.getEntite(1L)).thenReturn(equipe);
        when(equipeService.compterMembres(equipe)).thenReturn(1);
        when(equipeMapper.toResponse(eq(equipe), anyInt())).thenReturn(equipeResponse);

        ResponseEntity<EquipeResponse> resp = equipeController.getEquipe(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(equipeResponse, resp.getBody());
    }

    @Test
    void getMembres_returns200WithUsers() {
        when(equipeService.getMembres(1L)).thenReturn(List.of(membre));
        when(userMapper.toResponse(membre)).thenReturn(userResponse);

        ResponseEntity<List<UserResponse>> resp = equipeController.getMembres(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals(userResponse, resp.getBody().get(0));
    }
}
