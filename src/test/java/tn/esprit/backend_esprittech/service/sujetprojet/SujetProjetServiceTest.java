package tn.esprit.backend_esprittech.service.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SujetProjetServiceTest {

    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private SujetProjetMapper sujetProjetMapper;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private SujetReferenceService referenceService;
    @Mock private EquipeRechercheRepository equipeRechercheRepository;

    @InjectMocks private SujetProjetService service;

    private User enseignant;
    private SujetProjetRequest request;
    private SujetProjet entity;
    private SujetProjetResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        enseignant = User.builder().id(10L).nom("Ben Ali").prenom("Sami")
                .email("sami@esprit.tn").identifiant("ENS-1")
                .role(Role.ROLE_ENSEIGNANT).typeUtilisateur(TypeUtilisateur.ENSEIGNANT).build();

        request = new SujetProjetRequest("Sujet", CategorieSujet.PFE, "Desc", "Obj",
                List.of("Soft skills"), List.of("Info"), List.of("Java"), 2);

        entity = SujetProjet.builder().id(1L).titre("Sujet").categorie(CategorieSujet.PFE)
                .description("Desc").objectifs("Obj").prerequis("Soft skills").domaine("Info")
                .technologies("Java").capaciteAccueil(2).statut(StatutSujet.SOUMIS_EN_VALIDATION)
                .encadrantId(10L).dateCreation(LocalDateTime.now()).catalogue(false).build();

        response = new SujetProjetResponse(1L, "Sujet", CategorieSujet.PFE, "Desc", "Obj",
                List.of("Soft skills"), List.of("Info"), List.of("Java"), 2, StatutSujet.SOUMIS_EN_VALIDATION,
                null, false, false, 10L, "Sami Ben Ali", "sami@esprit.tn", null,
                entity.getDateCreation(), entity.getDateCreation(), null, null, null, null);

        when(userService.getCurrentUser()).thenReturn(enseignant);
        when(userRepository.findById(10L)).thenReturn(Optional.of(enseignant));
        when(userRepository.findAllById(any())).thenReturn(List.of(enseignant));
    }

    @Test
    void creerSujet_savesAndReturnsResponse() {
        when(sujetProjetMapper.toEntity(request, 10L)).thenReturn(entity);
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null)).thenReturn(response);

        assertEquals(response, service.creerSujet(request));
    }

    @Test
    void creerSujet_nonEnseignant_throwsAccessDenied() {
        enseignant.setRole(Role.ROLE_ETUDIANT);
        assertThrows(AccessDeniedException.class, () -> service.creerSujet(request));
    }

    @Test
    void getMesSujets_returnsSubjectsForCurrentTeacher() {
        when(sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(10L)).thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null)).thenReturn(response);

        assertEquals(1, service.getMesSujets(null, null).size());
    }

    @Test
    void getSujetById_owner_canAccess() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null)).thenReturn(response);

        assertEquals(response, service.getSujetById(1L));
    }

    @Test
    void getSujetById_notOwnerAndNotPublic_throwsAccessDenied() {
        entity.setEncadrantId(99L);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(AccessDeniedException.class, () -> service.getSujetById(1L));
    }
}
