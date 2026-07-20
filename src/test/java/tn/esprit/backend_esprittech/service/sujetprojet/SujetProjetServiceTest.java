package tn.esprit.backend_esprittech.service.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetInvalidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.candidature.AffectationRepository;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;
import tn.esprit.backend_esprittech.service.projetcatalogue.ProjetCatalogueService;

import java.lang.reflect.Method;
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
    @Mock private AffectationRepository affectationRepository;
    @Mock private CandidatureRepository candidatureRepository;
    @Mock private ProjetCatalogueService projetCatalogueService;
    @Mock private NotificationService notificationService;

    @InjectMocks private SujetProjetService service;

    private User enseignant;
    private SujetProjetRequest request;
    private SujetProjet entity;
    private SujetProjetResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        enseignant = User.builder().id(10L).nom("Ben Ali").prenom("Sami")
                .email("sami@esprit.tn")
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
                entity.getDateCreation(), entity.getDateCreation(), null, null, null, null, false, 0, 0);

        when(userService.getCurrentUser()).thenReturn(enseignant);
        when(userRepository.findById(10L)).thenReturn(Optional.of(enseignant));
        when(userRepository.findAllById(any())).thenReturn(List.of(enseignant));
        when(affectationRepository.countBySujetIdAndStatut(anyLong(), any())).thenReturn(0L);
        when(candidatureRepository.findBySujetIdAndEtudiantId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(affectationRepository.findBySujetIdAndEtudiantId(anyLong(), anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void creerSujet_savesAndReturnsResponse() {
        User chef = User.builder().id(20L).nom("Chef").prenom("Ahmed")
                .role(Role.ROLE_CHEF_EQUIPE).typeUtilisateur(TypeUtilisateur.ENSEIGNANT).build();
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("Equipe Test").chef(chef).build();
        enseignant.setEquipeRecherche(equipe);
        when(userRepository.findById(10L)).thenReturn(Optional.of(enseignant));
        when(sujetProjetMapper.toEntity(request, 10L)).thenReturn(entity);
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", "Equipe Test", 0)).thenReturn(response);

        assertEquals(response, service.creerSujet(request));

        verify(notificationService).creerNotification(
                eq(chef.getId()), eq(NotificationType.SUJET_DEMANDE),
                anyString(), anyString(), eq("/frontoffice/validation-sujets"));
    }

    @Test
    void creerSujet_nonEnseignant_throwsAccessDenied() {
        enseignant.setRole(Role.ROLE_ETUDIANT);
        assertThrows(AccessDeniedException.class, () -> service.creerSujet(request));
    }

    @Test
    void creerSujet_chefEquipe_autoValidatesSubject() {
        enseignant.setRole(Role.ROLE_CHEF_EQUIPE);
        when(equipeRechercheRepository.findByChefId(10L)).thenReturn(Optional.of(
                tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche.builder().id(1L).nom("AI Lab").build()
        ));
        when(sujetProjetMapper.toEntity(request, 10L)).thenReturn(entity);
        when(sujetProjetRepository.save(entity)).thenAnswer(invocation -> invocation.getArgument(0));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        service.creerSujet(request);

        assertEquals(StatutSujet.VALIDE, entity.getStatut());
        assertTrue(entity.getCatalogue());
        assertNotNull(entity.getDateValidation());
        assertNotNull(entity.getDateSoumission());
    }

    @Test
    void getMesSujets_returnsSubjectsForCurrentTeacher() {
        when(sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(10L)).thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null, 0)).thenReturn(response);

        assertEquals(1, service.getMesSujets(null, null).size());
    }

    @Test
    void getMesSujets_excludesTerminatedSubjectsPublishedToCatalogue() {
        SujetProjet termine = SujetProjet.builder()
                .id(2L)
                .titre("Termine")
                .categorie(CategorieSujet.PFE)
                .statut(StatutSujet.REALISATION_TERMINEE)
                .encadrantId(10L)
                .catalogue(true)
                .build();
        when(sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(10L))
                .thenReturn(List.of(entity, termine));
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null, 0)).thenReturn(response);

        List<SujetProjetResponse> result = service.getMesSujets(null, null);

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
    }

    @Test
    void getMesSujets_withTerminatedStatutFilter_returnsEmpty() {
        assertEquals(List.of(), service.getMesSujets(null, StatutSujet.REALISATION_TERMINEE));
        verify(sujetProjetRepository, never()).findByEncadrantIdAndStatutOrderByDateCreationDesc(any(), any());
    }

    @Test
    void getSujetById_owner_canAccess() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null, 0)).thenReturn(response);

        assertEquals(response, service.getSujetById(1L));
    }

    @Test
    void getSujetById_notOwnerAndNotPublic_throwsAccessDenied() {
        entity.setEncadrantId(99L);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(AccessDeniedException.class, () -> service.getSujetById(1L));
    }

    @Test
    void getSujetById_studentWithCandidature_canAccessTerminatedSubject() {
        User etudiant = User.builder().id(50L).nom("Etudiant").prenom("Ali")
                .email("ali@esprit.tn")
                .role(Role.ROLE_ETUDIANT).typeUtilisateur(TypeUtilisateur.ETUDIANT).build();
        when(userService.getCurrentUser()).thenReturn(etudiant);

        entity.setEncadrantId(10L);
        entity.setStatut(StatutSujet.REALISATION_TERMINEE);
        entity.setCatalogue(true);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(candidatureRepository.findBySujetIdAndEtudiantId(1L, 50L))
                .thenReturn(Optional.of(Candidature.builder()
                        .id(7L)
                        .sujetId(1L)
                        .etudiant(etudiant)
                        .build()));
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null, 0)).thenReturn(response);

        assertEquals(response, service.getSujetById(1L));
    }

    @Test
    void getSujetsPourCandidatures_returnsAllowedStatuses() {
        when(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(any())).thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        List<SujetProjetResponse> result = service.getSujetsPourCandidatures();

        assertEquals(List.of(response), result);
        verify(sujetProjetRepository).findByStatutInOrderByDateCreationDesc(List.of(
                StatutSujet.VALIDE,
                StatutSujet.CANDIDATURE_OUVERTE,
                StatutSujet.CANDIDATURE_FERMEE,
                StatutSujet.REALISATION_EN_COURS,
                StatutSujet.REALISATION_TERMINEE
        ));
    }

    @Test
    void getSujetsEnAttenteValidation_adminReturnsPendingSubjects() {
        enseignant.setRole(Role.ROLE_ADMIN);
        when(sujetProjetRepository.findByStatutOrderByDateCreationDesc(StatutSujet.SOUMIS_EN_VALIDATION))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        assertEquals(List.of(response), service.getSujetsEnAttenteValidation());
    }

    @Test
    void getSujetsEnAttenteValidation_nonChefDenied() {
        assertThrows(AccessDeniedException.class, () -> service.getSujetsEnAttenteValidation());
    }

    @Test
    void validerSujet_chefEquipeValidatesPendingSubject() {
        enseignant.setRole(Role.ROLE_CHEF_EQUIPE);
        entity.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("Equipe Test").build();
        when(equipeRechercheRepository.findByChefId(10L)).thenReturn(Optional.of(equipe));
        when(userRepository.findByEquipeRechercheId(1L)).thenReturn(List.of(enseignant));
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        service.validerSujet(1L);

        assertEquals(StatutSujet.VALIDE, entity.getStatut());
        assertTrue(entity.getCatalogue());
        assertNotNull(entity.getDateValidation());

        verify(notificationService).creerNotification(
                eq(10L), eq(NotificationType.SUJET_VALIDE),
                anyString(), anyString(), eq("/frontoffice/sujets/mes-sujets"));
    }

    @Test
    void validerSujet_nonPendingSubjectThrows() {
        enseignant.setRole(Role.ROLE_CHEF_EQUIPE);
        entity.setStatut(StatutSujet.VALIDE);
        when(equipeRechercheRepository.findByChefId(10L)).thenReturn(Optional.empty());
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.validerSujet(1L));
    }

    @Test
    void validerSujet_missingSubjectThrows() {
        enseignant.setRole(Role.ROLE_CHEF_EQUIPE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.validerSujet(1L));
    }

    @Test
    void validerSujet_nonChefDenied() {
        entity.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(AccessDeniedException.class, () -> service.validerSujet(1L));
    }

    @Test
    void invaliderSujet_chefInvalidatesWithReason() {
        enseignant.setRole(Role.ROLE_CHEF_EQUIPE);
        entity.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        EquipeRecherche equipe = EquipeRecherche.builder().id(1L).nom("Equipe Test").build();
        when(equipeRechercheRepository.findByChefId(10L)).thenReturn(Optional.of(equipe));
        when(userRepository.findByEquipeRechercheId(1L)).thenReturn(List.of(enseignant));
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        service.invaliderSujet(1L, new SujetInvalidationRequest("Sujet incomplet"));

        assertEquals(StatutSujet.INVALIDE, entity.getStatut());
        assertEquals("Sujet incomplet", entity.getMotifInvalidation());
        assertNotNull(entity.getDateValidation());

        verify(notificationService).creerNotification(
                eq(10L), eq(NotificationType.SUJET_REJETE),
                anyString(), anyString(), eq("/frontoffice/sujets/mes-sujets"));
    }

    @Test
    void invaliderSujet_nonPendingSubjectThrows() {
        enseignant.setRole(Role.ROLE_CHEF_EQUIPE);
        entity.setStatut(StatutSujet.VALIDE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        var invalidation = new SujetInvalidationRequest("Motif");
        assertThrows(IllegalArgumentException.class,
                () -> service.invaliderSujet(1L, invalidation));
    }

    @Test
    void getMesSujets_withCategorieAndStatutFiltersInMemory() {
        SujetProjet matching = SujetProjet.builder()
                .id(2L)
                .titre("Matching")
                .categorie(CategorieSujet.PFE)
                .statut(StatutSujet.VALIDE)
                .encadrantId(10L)
                .build();
        SujetProjet other = SujetProjet.builder()
                .id(3L)
                .titre("Other")
                .categorie(CategorieSujet.RDI)
                .statut(StatutSujet.VALIDE)
                .encadrantId(10L)
                .build();
        when(sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(10L)).thenReturn(List.of(matching, other));
        when(sujetProjetMapper.toResponse(eq(matching), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        List<SujetProjetResponse> result = service.getMesSujets(CategorieSujet.PFE, StatutSujet.VALIDE);

        assertEquals(List.of(response), result);
    }

    @Test
    void getMesSujets_withCategorieUsesRepositoryFilter() {
        when(sujetProjetRepository.findByEncadrantIdAndCategorieOrderByDateCreationDesc(10L, CategorieSujet.PFE))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        assertEquals(List.of(response), service.getMesSujets(CategorieSujet.PFE, null));
    }

    @Test
    void getMesSujets_withStatutUsesRepositoryFilter() {
        when(sujetProjetRepository.findByEncadrantIdAndStatutOrderByDateCreationDesc(10L, StatutSujet.SOUMIS_EN_VALIDATION))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        assertEquals(List.of(response), service.getMesSujets(null, StatutSujet.SOUMIS_EN_VALIDATION));
    }

    @Test
    void getSujetsDisponibles_withoutCategorieReturnsAllAvailable() {
        when(sujetProjetRepository.findByCatalogueTrueAndStatutInOrderByDateCreationDesc(any())).thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        assertEquals(List.of(response), service.getSujetsDisponibles(null));
    }

    @Test
    void getSujetsDisponibles_withCategorieFiltersResults() {
        SujetProjet matching = SujetProjet.builder().id(2L).categorie(CategorieSujet.PFE)
                .statut(StatutSujet.VALIDE).encadrantId(10L).build();
        SujetProjet other = SujetProjet.builder().id(3L).categorie(CategorieSujet.RDI)
                .statut(StatutSujet.VALIDE).encadrantId(10L).build();
        when(sujetProjetRepository.findByCatalogueTrueAndStatutInOrderByDateCreationDesc(any())).thenReturn(List.of(matching, other));
        when(sujetProjetMapper.toResponse(eq(matching), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        assertEquals(List.of(response), service.getSujetsDisponibles(CategorieSujet.PFE));
    }

    @Test
    void getSujetById_publicSubjectCanBeReadByNonOwner() {
        entity.setEncadrantId(99L);
        entity.setCatalogue(true);
        entity.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetMapper.toResponse(entity, "Encadrant inconnu", null, null, 0)).thenReturn(response);

        assertEquals(response, service.getSujetById(1L));
    }

    @Test
    void getSujetById_missingSubjectThrows() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getSujetById(1L));
    }

    @Test
    void modifierSujet_ownerUpdatesSubject() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null, 0)).thenReturn(response);

        assertEquals(response, service.modifierSujet(1L, request));

        verify(sujetProjetMapper).updateEntity(entity, request);
    }

    @Test
    void modifierSujet_nonOwnerThrows() {
        entity.setEncadrantId(99L);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.modifierSujet(1L, request));
    }

    @Test
    void supprimerSujet_ownerDeletesSubject() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.supprimerSujet(1L);

        verify(sujetProjetRepository).delete(entity);
    }

    @Test
    void supprimerSujet_nonOwnerThrows() {
        entity.setEncadrantId(99L);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.supprimerSujet(1L));
    }

    @Test
    void suggestionsDelegateToReferenceService() {
        when(referenceService.getAllTechnologieNoms()).thenReturn(List.of("Java"));
        when(referenceService.getAllPrerequisNoms()).thenReturn(List.of("Git"));
        when(referenceService.getAllDomaineNoms()).thenReturn(List.of("IA"));

        assertEquals(List.of("Java"), service.getTechnologiesDisponibles());
        assertEquals(List.of("Git"), service.getPrerequisSuggestions());
        assertEquals(List.of("IA"), service.getDomainesSuggestions());
    }

    @Test
    void toResponse_unknownEncadrantUsesFallbackName() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        when(sujetProjetMapper.toResponse(entity, "Encadrant inconnu", null, null, 0)).thenReturn(response);

        assertEquals(response, service.getSujetById(1L));
    }

    @Test
    void getProjetsEvaluables_adminReturnsFinishedProjects() {
        enseignant.setRole(Role.ROLE_ADMIN);
        entity.setStatut(StatutSujet.REALISATION_TERMINEE);
        when(sujetProjetRepository.findByStatutOrderByDateCreationDesc(StatutSujet.REALISATION_TERMINEE))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        assertEquals(List.of(response), service.getProjetsEvaluables());
    }

    @Test
    void getProjetsEvaluables_nonAdminDenied() {
        assertThrows(AccessDeniedException.class, () -> service.getProjetsEvaluables());
    }

    @Test
    void declarerTerminaison_subjectInProgressBecomesFinished() {
        entity.setStatut(StatutSujet.REALISATION_EN_COURS);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null, 0)).thenReturn(response);

        assertEquals(response, service.declarerTerminaison(1L));
        assertEquals(StatutSujet.REALISATION_TERMINEE, entity.getStatut());
        assertEquals(Boolean.TRUE, entity.getCatalogue());
        assertNotNull(entity.getDateTerminaison());
        verify(projetCatalogueService).publierDepuisSujetTermine(entity, null, null);
    }

    @Test
    void declarerTerminaison_missingSubjectThrows() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.declarerTerminaison(1L));
    }

    @Test
    void declarerTerminaison_wrongStatusThrows() {
        entity.setStatut(StatutSujet.VALIDE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalStateException.class, () -> service.declarerTerminaison(1L));
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsSujetFields() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        var result = service.getOldValues(1L);

        assertEquals(entity.getStatut().name(), result.get("statut"));
        assertEquals(entity.getTitre(), result.get("titre"));
        assertEquals(entity.getCatalogue(), result.get("catalogue"));
    }

    @Test
    void getOldValues_sujetNotFound_returnsEmptyMap() {
        when(sujetProjetRepository.findById(99L)).thenReturn(Optional.empty());

        var result = service.getOldValues(99L);

        assertTrue(result.isEmpty());
    }

    // ---- getTitreById ----

    @Test
    void getTitreById_found_returnsTitre() {
        SujetProjet sujet = new SujetProjet();
        sujet.setId(1L);
        sujet.setTitre("Mon Super Projet");
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(sujet));

        assertEquals("Mon Super Projet", service.getTitreById(1L));
    }

    @Test
    void getTitreById_notFound_returnsInconnu() {
        when(sujetProjetRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("Inconnu", service.getTitreById(99L));
    }

    @Test
    void validerSujetAsAdmin_validatesSubject() {
        enseignant.setRole(Role.ROLE_ADMIN);
        entity.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        when(userRepository.findById(10L)).thenReturn(Optional.of(enseignant));
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        SujetProjetResponse result = service.validerSujetAsAdmin(1L);

        assertEquals(response, result);
        assertEquals(StatutSujet.VALIDE, entity.getStatut());
        assertTrue(entity.getCatalogue());
        assertNotNull(entity.getDateValidation());

        verify(notificationService).creerNotification(
                eq(10L), eq(NotificationType.SUJET_VALIDE),
                anyString(), anyString(), eq("/frontoffice/sujets/mes-sujets"));
    }

    @Test
    void invaliderSujetAsAdmin_invalidatesSubject() {
        enseignant.setRole(Role.ROLE_ADMIN);
        entity.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        when(userRepository.findById(10L)).thenReturn(Optional.of(enseignant));
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull(), eq(0))).thenReturn(response);

        SujetInvalidationRequest invReq = new SujetInvalidationRequest("Non conforme");
        SujetProjetResponse result = service.invaliderSujetAsAdmin(1L, invReq);

        assertEquals(response, result);
        assertEquals(StatutSujet.INVALIDE, entity.getStatut());
        assertEquals("Non conforme", entity.getMotifInvalidation());
        assertNotNull(entity.getDateValidation());

        verify(notificationService).creerNotification(
                eq(10L), eq(NotificationType.SUJET_REJETE),
                anyString(), anyString(), eq("/frontoffice/sujets/mes-sujets"));
    }
}
