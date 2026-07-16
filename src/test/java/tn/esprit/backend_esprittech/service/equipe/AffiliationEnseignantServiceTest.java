package tn.esprit.backend_esprittech.service.equipe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.backend_esprittech.dto.equipe.AffiliationEnseignantResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.AffiliationEnseignant;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.repository.equipe.AffiliationEnseignantRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.service.notification.NotificationService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AffiliationEnseignantServiceTest {

    @Mock
    private AffiliationEnseignantRepository affiliationRepository;
    @Mock
    private EquipeRechercheRepository equipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AffiliationEnseignantService service;

    private User enseignant;
    private User chef;
    private User admin;
    private EquipeRecherche equipe;
    private UserResponse enseignantResponse;
    private AffiliationEnseignant savedAffiliation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        enseignant = new User();
        enseignant.setId(1L);
        enseignant.setNom("Doe");
        enseignant.setPrenom("John");
        enseignant.setEmail("john@test.tn");
        enseignant.setRole(Role.ROLE_ENSEIGNANT);
        enseignant.setTypeUtilisateur(TypeUtilisateur.ENSEIGNANT);
        enseignant.setEnabled(true);
        enseignant.setCreatedAt(Instant.now());

        chef = new User();
        chef.setId(2L);
        chef.setNom("Chef");
        chef.setPrenom("Pierre");
        chef.setEmail("chef@test.tn");
        chef.setRole(Role.ROLE_CHEF_EQUIPE);
        chef.setTypeUtilisateur(TypeUtilisateur.ENSEIGNANT);
        chef.setEnabled(true);

        admin = new User();
        admin.setId(3L);
        admin.setNom("Admin");
        admin.setRole(Role.ROLE_ADMIN);

        equipe = new EquipeRecherche();
        equipe.setId(10L);
        equipe.setNom("Équipe Alpha");
        equipe.setChef(chef);

        enseignantResponse = new UserResponse(1L, "Doe", "John", "john@test.tn",
                Role.ROLE_ENSEIGNANT, TypeUtilisateur.ENSEIGNANT,
                "Informatique", true, Instant.now(), false, null, null);

        savedAffiliation = new AffiliationEnseignant();
        savedAffiliation.setId(100L);
        savedAffiliation.setEnseignant(enseignant);
        savedAffiliation.setEquipe(equipe);
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);
        savedAffiliation.setDateDemande(LocalDateTime.now());
    }

    // =========================================================================
    // demander()
    // =========================================================================

    @Test
    void demander_success() {
        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(affiliationRepository.existsByEnseignantIdAndEquipeIdAndStatut(1L, 10L, StatutAffiliation.EN_ATTENTE))
                .thenReturn(false);
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        AffiliationEnseignantResponse response = service.demander(10L, enseignant);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(10L, response.equipeId());
        assertEquals("Équipe Alpha", response.equipeNom());
        assertEquals(StatutAffiliation.EN_ATTENTE, response.statut());
        verify(affiliationRepository).save(any());
        verify(notificationService).creerNotification(
                eq(chef.getId()), eq(NotificationType.AFFILIATION_DEMANDE),
                eq("Demande d'affiliation"), anyString(),
                eq("/frontoffice/equipes-recherche/demandes"));
    }

    @Test
    void demander_wrongRole_throwsBusinessException() {
        User etudiant = new User();
        etudiant.setId(5L);
        etudiant.setRole(Role.ROLE_ETUDIANT);

        assertThrows(BusinessException.class, () -> service.demander(10L, etudiant));
        verify(affiliationRepository, never()).save(any());
    }

    @Test
    void demander_equipeNotFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.demander(99L, enseignant));
    }

    @Test
    void demander_alreadyMember_throwsBusinessException() {
        enseignant.setEquipeRecherche(equipe);
        equipe.setId(10L);

        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));

        assertThrows(BusinessException.class, () -> service.demander(10L, enseignant));
    }

    @Test
    void demander_enseignantAlreadyInDifferentTeam_proceeds() {
        EquipeRecherche autreEquipe = new EquipeRecherche();
        autreEquipe.setId(99L);
        enseignant.setEquipeRecherche(autreEquipe);

        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(affiliationRepository.existsByEnseignantIdAndEquipeIdAndStatut(1L, 10L, StatutAffiliation.EN_ATTENTE))
                .thenReturn(false);
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        AffiliationEnseignantResponse response = service.demander(10L, enseignant);

        assertNotNull(response);
        assertEquals(StatutAffiliation.EN_ATTENTE, response.statut());
        verify(affiliationRepository).save(any());
    }

    @Test
    void demander_duplicatePending_throwsBusinessException() {
        when(equipeRepository.findById(10L)).thenReturn(Optional.of(equipe));
        when(affiliationRepository.existsByEnseignantIdAndEquipeIdAndStatut(1L, 10L, StatutAffiliation.EN_ATTENTE))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.demander(10L, enseignant));
    }

    // =========================================================================
    // traiter()
    // =========================================================================

    @Test
    void traiter_accept_success() {
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);
        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(equipeRepository.save(any())).thenReturn(equipe);
        when(userRepository.save(any())).thenReturn(enseignant);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        AffiliationEnseignantResponse response = service.traiter(100L, 10L, chef,
                StatutAffiliation.ACCEPTEE, "Bienvenue dans l'équipe");

        assertNotNull(response);
        assertEquals(StatutAffiliation.ACCEPTEE, savedAffiliation.getStatut());
        assertNotNull(savedAffiliation.getDateDecision());
        assertEquals("Bienvenue dans l'équipe", savedAffiliation.getMotifDecision());
        verify(equipeRepository).save(equipe);
        verify(userRepository).save(enseignant);
        verify(notificationService).creerNotification(
                eq(enseignant.getId()), eq(NotificationType.AFFILIATION_ACCEPTEE),
                eq("Affiliation acceptee"), anyString(),
                eq("/frontoffice/equipes-recherche/mon-equipe"));
    }

    @Test
    void traiter_refuse_success() {
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);
        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        AffiliationEnseignantResponse response = service.traiter(100L, 10L, chef,
                StatutAffiliation.REFUSEE, "Motif de refus");

        assertNotNull(response);
        assertEquals(StatutAffiliation.REFUSEE, savedAffiliation.getStatut());
        assertNotNull(savedAffiliation.getDateDecision());
        assertEquals("Motif de refus", savedAffiliation.getMotifDecision());
        verify(equipeRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(notificationService).creerNotification(
                eq(enseignant.getId()), eq(NotificationType.AFFILIATION_REJETEE),
                eq("Affiliation refusee"), anyString(),
                eq("/frontoffice/equipes-recherche/mes-demandes"));
    }

    @Test
    void traiter_wrongRole_throwsBusinessException() {
        User etudiant = new User();
        etudiant.setRole(Role.ROLE_ETUDIANT);

        assertThrows(BusinessException.class,
                () -> service.traiter(100L, 10L, etudiant, StatutAffiliation.ACCEPTEE, null));
    }

    @Test
    void traiter_notFound_throwsResourceNotFoundException() {
        when(affiliationRepository.findByIdAndEquipeId(999L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.traiter(999L, 10L, admin, StatutAffiliation.ACCEPTEE, null));
    }

    @Test
    void traiter_notChef_throwsBusinessException() {
        User autreChef = new User();
        autreChef.setId(99L);
        autreChef.setRole(Role.ROLE_CHEF_EQUIPE);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));

        assertThrows(BusinessException.class,
                () -> service.traiter(100L, 10L, autreChef, StatutAffiliation.ACCEPTEE, null));
    }

    @Test
    void traiter_nonAdminChefIsNull_throwsBusinessException() {
        equipe.setChef(null);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));

        assertThrows(BusinessException.class,
                () -> service.traiter(100L, 10L, chef, StatutAffiliation.ACCEPTEE, null));
    }

    @Test
    void traiter_adminCanBypassChefCheck() {
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);
        equipe.setChef(null);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        AffiliationEnseignantResponse response = service.traiter(100L, 10L, admin,
                StatutAffiliation.REFUSEE, "Admin decision");

        assertNotNull(response);
        assertEquals(StatutAffiliation.REFUSEE, savedAffiliation.getStatut());
    }

    @Test
    void traiter_alreadyProcessed_throwsBusinessException() {
        savedAffiliation.setStatut(StatutAffiliation.ACCEPTEE);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));

        assertThrows(BusinessException.class,
                () -> service.traiter(100L, 10L, admin, StatutAffiliation.ACCEPTEE, null));
    }

    @Test
    void traiter_accept_enseignantAlreadyInThisTeam_proceeds() {
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);
        enseignant.setEquipeRecherche(equipe);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(equipeRepository.save(any())).thenReturn(equipe);
        when(userRepository.save(any())).thenReturn(enseignant);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        AffiliationEnseignantResponse response = service.traiter(100L, 10L, admin,
                StatutAffiliation.ACCEPTEE, null);

        assertNotNull(response);
        assertEquals(StatutAffiliation.ACCEPTEE, savedAffiliation.getStatut());
        verify(equipeRepository).save(equipe);
        verify(userRepository).save(enseignant);
    }

    @Test
    void traiter_accept_enseignantAlreadyInAnotherTeam_throwsBusinessException() {
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);
        EquipeRecherche autreEquipe = new EquipeRecherche();
        autreEquipe.setId(99L);
        enseignant.setEquipeRecherche(autreEquipe);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));

        assertThrows(BusinessException.class,
                () -> service.traiter(100L, 10L, admin, StatutAffiliation.ACCEPTEE, null));
    }

    @Test
    void traiter_accept_autoRejectsOtherPendingRequests() {
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);

        EquipeRecherche autreEquipe = new EquipeRecherche();
        autreEquipe.setId(99L);

        AffiliationEnseignant otherPending = new AffiliationEnseignant();
        otherPending.setId(200L);
        otherPending.setEnseignant(enseignant);
        otherPending.setEquipe(autreEquipe);
        otherPending.setStatut(StatutAffiliation.EN_ATTENTE);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));
        when(affiliationRepository.findByEnseignantIdAndStatut(1L, StatutAffiliation.EN_ATTENTE))
                .thenReturn(List.of(otherPending));
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(equipeRepository.save(any())).thenReturn(equipe);
        when(userRepository.save(any())).thenReturn(enseignant);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        service.traiter(100L, 10L, admin, StatutAffiliation.ACCEPTEE, "Bienvenue");

        assertEquals(StatutAffiliation.REFUSEE, otherPending.getStatut());
        assertNotNull(otherPending.getDateDecision());
        assertEquals("Deja accepte dans une autre equipe", otherPending.getMotifDecision());
        verify(affiliationRepository).saveAll(List.of(otherPending));
    }

    @Test
    void traiter_accept_noOtherPendingRequests_savesEmptyList() {
        savedAffiliation.setStatut(StatutAffiliation.EN_ATTENTE);

        when(affiliationRepository.findByIdAndEquipeId(100L, 10L)).thenReturn(Optional.of(savedAffiliation));
        when(affiliationRepository.findByEnseignantIdAndStatut(1L, StatutAffiliation.EN_ATTENTE))
                .thenReturn(List.of());
        when(affiliationRepository.save(any())).thenReturn(savedAffiliation);
        when(equipeRepository.save(any())).thenReturn(equipe);
        when(userRepository.save(any())).thenReturn(enseignant);
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        service.traiter(100L, 10L, admin, StatutAffiliation.ACCEPTEE, null);

        verify(affiliationRepository).saveAll(List.of());
    }

    // =========================================================================
    // getAllDemandes()
    // =========================================================================

    @Test
    void getAllDemandes_withResults() {
        when(affiliationRepository.findAllByOrderByDateDemandeDesc())
                .thenReturn(List.of(savedAffiliation));
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        List<AffiliationEnseignantResponse> result = service.getAllDemandes();

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
        verify(affiliationRepository).findAllByOrderByDateDemandeDesc();
    }

    @Test
    void getAllDemandes_empty_returnsEmptyList() {
        when(affiliationRepository.findAllByOrderByDateDemandeDesc())
                .thenReturn(List.of());

        List<AffiliationEnseignantResponse> result = service.getAllDemandes();

        assertTrue(result.isEmpty());
        verify(affiliationRepository).findAllByOrderByDateDemandeDesc();
    }

    // =========================================================================
    // getDemandesByEquipe()
    // =========================================================================

    @Test
    void getDemandesByEquipe_withResults() {
        when(affiliationRepository.findByEquipeIdOrderByDateDemandeDesc(10L))
                .thenReturn(List.of(savedAffiliation));
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        List<AffiliationEnseignantResponse> result = service.getDemandesByEquipe(10L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
    }

    @Test
    void getDemandesByEquipe_empty_returnsEmptyList() {
        when(affiliationRepository.findByEquipeIdOrderByDateDemandeDesc(10L))
                .thenReturn(List.of());

        List<AffiliationEnseignantResponse> result = service.getDemandesByEquipe(10L);

        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // getMesDemandes()
    // =========================================================================

    @Test
    void getMesDemandes_withResults() {
        when(affiliationRepository.findByEnseignantIdOrderByDateDemandeDesc(1L))
                .thenReturn(List.of(savedAffiliation));
        when(userMapper.toResponse(enseignant)).thenReturn(enseignantResponse);

        List<AffiliationEnseignantResponse> result = service.getMesDemandes(enseignant);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
    }

    @Test
    void getMesDemandes_empty_returnsEmptyList() {
        when(affiliationRepository.findByEnseignantIdOrderByDateDemandeDesc(1L))
                .thenReturn(List.of());

        List<AffiliationEnseignantResponse> result = service.getMesDemandes(enseignant);

        assertTrue(result.isEmpty());
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsAffiliationFields() {
        savedAffiliation.getEnseignant().setId(1L);
        savedAffiliation.getEquipe().setId(10L);
        savedAffiliation.setMotifDecision("Approuve");
        when(affiliationRepository.findById(100L)).thenReturn(Optional.of(savedAffiliation));

        var result = service.getOldValues(100L);

        assertEquals(savedAffiliation.getStatut().name(), result.get("statut"));
        assertEquals(1L, result.get("enseignantId"));
        assertEquals(10L, result.get("equipeId"));
        assertEquals("Approuve", result.get("motifDecision"));
    }

    // ---- getLabelById ----

    @Test
    void getLabelById_found_returnsLabel() {
        when(affiliationRepository.findById(100L)).thenReturn(Optional.of(savedAffiliation));

        assertEquals("John Doe - équipe Équipe Alpha", service.getLabelById(100L));
    }

    @Test
    void getLabelById_notFound_returnsInconnu() {
        when(affiliationRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("Inconnu", service.getLabelById(99L));
    }
}
