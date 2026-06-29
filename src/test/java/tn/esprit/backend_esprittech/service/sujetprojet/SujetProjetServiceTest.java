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
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Test
    void getSujetsPourCandidatures_returnsAllowedStatuses() {
        when(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(any())).thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

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
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

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
        when(equipeRechercheRepository.findByChefId(10L)).thenReturn(Optional.empty());
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

        service.validerSujet(1L);

        assertEquals(StatutSujet.CANDIDATURE_OUVERTE, entity.getStatut());
        assertTrue(entity.getCatalogue());
        assertNotNull(entity.getDateValidation());
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
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

        service.invaliderSujet(1L, new SujetInvalidationRequest("Sujet incomplet"));

        assertEquals(StatutSujet.INVALIDE, entity.getStatut());
        assertEquals("Sujet incomplet", entity.getMotifInvalidation());
        assertNotNull(entity.getDateValidation());
    }

    @Test
    void invaliderSujet_nonPendingSubjectThrows() {
        enseignant.setRole(Role.ROLE_CHEF_EQUIPE);
        entity.setStatut(StatutSujet.VALIDE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class,
                () -> service.invaliderSujet(1L, new SujetInvalidationRequest("Motif")));
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
        when(sujetProjetMapper.toResponse(eq(matching), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

        List<SujetProjetResponse> result = service.getMesSujets(CategorieSujet.PFE, StatutSujet.VALIDE);

        assertEquals(List.of(response), result);
    }

    @Test
    void getMesSujets_withCategorieUsesRepositoryFilter() {
        when(sujetProjetRepository.findByEncadrantIdAndCategorieOrderByDateCreationDesc(10L, CategorieSujet.PFE))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

        assertEquals(List.of(response), service.getMesSujets(CategorieSujet.PFE, null));
    }

    @Test
    void getMesSujets_withStatutUsesRepositoryFilter() {
        when(sujetProjetRepository.findByEncadrantIdAndStatutOrderByDateCreationDesc(10L, StatutSujet.SOUMIS_EN_VALIDATION))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

        assertEquals(List.of(response), service.getMesSujets(null, StatutSujet.SOUMIS_EN_VALIDATION));
    }

    @Test
    void getSujetsDisponibles_withoutCategorieReturnsAllAvailable() {
        when(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(any())).thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

        assertEquals(List.of(response), service.getSujetsDisponibles(null));
    }

    @Test
    void getSujetsDisponibles_withCategorieFiltersResults() {
        SujetProjet matching = SujetProjet.builder().id(2L).categorie(CategorieSujet.PFE)
                .statut(StatutSujet.VALIDE).encadrantId(10L).build();
        SujetProjet other = SujetProjet.builder().id(3L).categorie(CategorieSujet.RDI)
                .statut(StatutSujet.VALIDE).encadrantId(10L).build();
        when(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(any())).thenReturn(List.of(matching, other));
        when(sujetProjetMapper.toResponse(eq(matching), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

        assertEquals(List.of(response), service.getSujetsDisponibles(CategorieSujet.PFE));
    }

    @Test
    void getSujetById_publicSubjectCanBeReadByNonOwner() {
        entity.setEncadrantId(99L);
        entity.setCatalogue(true);
        entity.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetMapper.toResponse(entity, "Encadrant inconnu", null, null)).thenReturn(response);

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
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null)).thenReturn(response);

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
        when(sujetProjetMapper.toResponse(entity, "Encadrant inconnu", null, null)).thenReturn(response);

        assertEquals(response, service.getSujetById(1L));
    }

    @Test
    void getProjetsEvaluables_adminReturnsFinishedProjects() {
        enseignant.setRole(Role.ROLE_ADMIN);
        entity.setStatut(StatutSujet.REALISATION_TERMINEE);
        when(sujetProjetRepository.findByStatutOrderByDateCreationDesc(StatutSujet.REALISATION_TERMINEE))
                .thenReturn(List.of(entity));
        when(sujetProjetMapper.toResponse(eq(entity), eq("Sami Ben Ali"), eq("sami@esprit.tn"), isNull())).thenReturn(response);

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
        when(sujetProjetMapper.toResponse(entity, "Sami Ben Ali", "sami@esprit.tn", null)).thenReturn(response);

        assertEquals(response, service.declarerTerminaison(1L));
        assertEquals(StatutSujet.REALISATION_TERMINEE, entity.getStatut());
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

    @Test
    @SuppressWarnings("unchecked")
    void getMembresEquipeIds_privateHelperReturnsChefIdForBothBranches() throws Exception {
        Method method = SujetProjetService.class.getDeclaredMethod("getMembresEquipeIds", User.class);
        method.setAccessible(true);

        when(equipeRechercheRepository.existsByChefId(10L)).thenReturn(false, true);

        assertEquals(Set.of(10L), (Set<Long>) method.invoke(service, enseignant));
        assertEquals(Set.of(10L), (Set<Long>) method.invoke(service, enseignant));
    }
}
