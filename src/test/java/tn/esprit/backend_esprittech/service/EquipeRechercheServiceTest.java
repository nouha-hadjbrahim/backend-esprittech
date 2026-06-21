package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRequest;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipeRechercheServiceTest {

    @Mock private EquipeRechercheRepository equipeRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private EquipeRechercheService equipeService;

    private User enseignant;

    @BeforeEach
    void setUp() {
        enseignant = User.builder()
                .id(5L).nom("Prof").prenom("Test")
                .email("prof@esprit.tn").identifiant("EA-5")
                .role(Role.ROLE_ENSEIGNANT).typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                .enabled(true).build();
    }

    // ---- creerEquipe ----

    @Test
    void creerEquipe_success_promotesChefAndSaves() {
        var request = new CreateEquipeRequest("AI Lab", "desc", 5L);
        when(equipeRepository.existsByNom("AI Lab")).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(enseignant));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipeRepository.save(any(EquipeRecherche.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipeRecherche result = equipeService.creerEquipe(request);

        assertEquals("AI Lab", result.getNom());
        assertEquals(enseignant, result.getChef());
        assertEquals(Role.ROLE_CHEF_EQUIPE, enseignant.getRole());
        verify(equipeRepository).save(any(EquipeRecherche.class));
    }

    @Test
    void creerEquipe_nomExists_throws() {
        var request = new CreateEquipeRequest("AI Lab", "desc", 5L);
        when(equipeRepository.existsByNom("AI Lab")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> equipeService.creerEquipe(request));
        verify(equipeRepository, never()).save(any());
    }

    @Test
    void creerEquipe_chefNotFound_throws() {
        var request = new CreateEquipeRequest("AI Lab", "desc", 99L);
        when(equipeRepository.existsByNom("AI Lab")).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipeService.creerEquipe(request));
    }

    @Test
    void creerEquipe_chefNotEnseignant_throws() {
        enseignant.setRole(Role.ROLE_ETUDIANT);
        var request = new CreateEquipeRequest("AI Lab", "desc", 5L);
        when(equipeRepository.existsByNom("AI Lab")).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(enseignant));

        assertThrows(IllegalArgumentException.class, () -> equipeService.creerEquipe(request));
        verify(equipeRepository, never()).save(any());
    }

    // ---- assignerChef ----

    @Test
    void assignerChef_success() {
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("AI Lab").build();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(equipe));
        when(userRepository.findById(5L)).thenReturn(Optional.of(enseignant));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipeRepository.save(any(EquipeRecherche.class))).thenAnswer(inv -> inv.getArgument(0));

        EquipeRecherche result = equipeService.assignerChef(1L, 5L);

        assertEquals(enseignant, result.getChef());
        assertEquals(Role.ROLE_CHEF_EQUIPE, enseignant.getRole());
    }

    @Test
    void assignerChef_equipeNotFound_throws() {
        when(equipeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipeService.assignerChef(1L, 5L));
    }

    // ---- getEntite / listerToutes ----

    @Test
    void getEntite_notFound_throws() {
        when(equipeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> equipeService.getEntite(1L));
    }

    @Test
    void listerToutes_returnsAll() {
        var e1 = EquipeRecherche.builder().id(1L).nom("A").build();
        var e2 = EquipeRecherche.builder().id(2L).nom("B").build();
        when(equipeRepository.findAll()).thenReturn(List.of(e1, e2));

        assertEquals(2, equipeService.listerToutes().size());
    }

    // ---- membres ----

    @Test
    void getMembres_withChef_returnsChef() {
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("A").chef(enseignant).build();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(equipe));

        List<User> membres = equipeService.getMembres(1L);

        assertEquals(1, membres.size());
        assertEquals(enseignant, membres.get(0));
    }

    @Test
    void getMembres_noChef_returnsEmpty() {
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("A").build();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(equipe));

        assertTrue(equipeService.getMembres(1L).isEmpty());
    }

    @Test
    void compterMembres_withAndWithoutChef() {
        EquipeRecherche avecChef = EquipeRecherche.builder().id(1L).nom("A").chef(enseignant).build();
        EquipeRecherche sansChef = EquipeRecherche.builder().id(2L).nom("B").build();

        assertEquals(1, equipeService.compterMembres(avecChef));
        assertEquals(0, equipeService.compterMembres(sansChef));
    }
}
