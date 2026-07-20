package tn.esprit.backend_esprittech.service.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.AdminCreateSujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetInvalidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.candidature.AffectationRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SujetProjetAdminServiceTest {

    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private SujetProjetMapper sujetProjetMapper;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private SujetReferenceService referenceService;
    @Mock private EquipeRechercheRepository equipeRechercheRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private SujetProjetService service;

    private User admin;
    private SujetProjet entity;
    private SujetProjetResponse response;
    private SujetProjetRequest request;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        admin = User.builder()
                .id(1L).nom("Admin").prenom("User")
                .email("admin@esprit.tn").role(Role.ROLE_ADMIN)
                .build();

        entity = SujetProjet.builder()
                .id(1L).titre("Sujet").categorie(CategorieSujet.PFE)
                .description("Desc").objectifs("Obj").prerequis("Soft skills")
                .domaine("Info").technologies("Java").capaciteAccueil(2)
                .statut(StatutSujet.SOUMIS_EN_VALIDATION)
                .encadrantId(10L).dateCreation(LocalDateTime.now())
                .catalogue(false).build();

        response = new SujetProjetResponse(
                1L, "Sujet", CategorieSujet.PFE, "Desc", "Obj",
                List.of("Soft skills"), List.of("Info"), List.of("Java"), 2,
                StatutSujet.SOUMIS_EN_VALIDATION, null, false, false,
                10L, "Sami Ben Ali", "sami@esprit.tn", null,
                entity.getDateCreation(), entity.getDateCreation(), null, null, null, null, false, 0, 0);

        request = new SujetProjetRequest(
                "Sujet modifié", CategorieSujet.PFE, "Desc", "Obj",
                List.of("Soft skills"), List.of("Info"), List.of("Java"), 2);

        pageable = PageRequest.of(0, 8);

        when(userService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(10L)).thenReturn(Optional.of(
                User.builder().id(10L).nom("Ben Ali").prenom("Sami").email("sami@esprit.tn").build()));
        when(affectationRepository.countBySujetIdAndStatut(anyLong(), any())).thenReturn(0L);
        when(sujetProjetMapper.toResponse(any(SujetProjet.class), anyString(), anyString(), nullable(String.class), anyInt()))
                .thenReturn(response);
    }

    @Test
    void listSujetsForAdmin_returnsPagedResults() {
        Page<SujetProjet> page = new PageImpl<>(List.of(entity));
        when(sujetProjetRepository.searchForAdmin(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<SujetProjetResponse> result = service.listSujetsForAdmin(null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(sujetProjetRepository).searchForAdmin(isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void listSujetsForAdmin_nonAdmin_throwsAccessDenied() {
        when(userService.getCurrentUser()).thenReturn(
                User.builder().id(2L).role(Role.ROLE_ENSEIGNANT).build());

        assertThrows(AccessDeniedException.class,
                () -> service.listSujetsForAdmin(null, null, null, pageable));
    }

    @Test
    void listDemandesForAdmin_returnsPendingSubjects() {
        Page<SujetProjet> page = new PageImpl<>(List.of(entity));
        when(sujetProjetRepository.searchDemandesForAdmin(anyList(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<SujetProjetResponse> result = service.listDemandesForAdmin(null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(sujetProjetRepository).searchDemandesForAdmin(
                argThat(statuts -> statuts.contains(StatutSujet.SOUMIS_EN_VALIDATION)
                        && statuts.contains(StatutSujet.EN_ATTENTE)),
                isNull(), isNull(), eq(pageable));
    }

    @Test
    void listSujetsDisponiblesForAdmin_returnsCatalogueSubjects() {
        entity.setCatalogue(true);
        entity.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        Page<SujetProjet> page = new PageImpl<>(List.of(entity));
        when(sujetProjetRepository.searchDisponiblesForAdmin(anyList(), isNull(), isNull(), eq(pageable)))
                .thenReturn(page);

        Page<SujetProjetResponse> result = service.listSujetsDisponiblesForAdmin(null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(sujetProjetRepository).searchDisponiblesForAdmin(
                argThat(statuts -> statuts.contains(StatutSujet.VALIDE)
                        && statuts.contains(StatutSujet.CANDIDATURE_OUVERTE)),
                isNull(), isNull(), eq(pageable));
    }

    @Test
    void validerSujetAsAdmin_updatesStatusAndCatalogue() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(any(SujetProjet.class))).thenAnswer(inv -> inv.getArgument(0));

        SujetProjetResponse result = service.validerSujetAsAdmin(1L);

        assertNotNull(result);
        assertEquals(StatutSujet.VALIDE, entity.getStatut());
        assertTrue(entity.getCatalogue());
        assertNull(entity.getMotifInvalidation());
        assertNotNull(entity.getDateValidation());
        verify(sujetProjetRepository).save(entity);
    }

    @Test
    void validerSujetAsAdmin_notPending_throwsIllegalArgument() {
        entity.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.validerSujetAsAdmin(1L));
    }

    @Test
    void validerSujetAsAdmin_notFound_throwsIllegalArgument() {
        when(sujetProjetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.validerSujetAsAdmin(99L));
    }

    @Test
    void invaliderSujetAsAdmin_setsMotifAndStatus() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(any(SujetProjet.class))).thenAnswer(inv -> inv.getArgument(0));
        SujetInvalidationRequest invalidationRequest = new SujetInvalidationRequest("  Motif de refus  ");

        service.invaliderSujetAsAdmin(1L, invalidationRequest);

        assertEquals(StatutSujet.INVALIDE, entity.getStatut());
        assertFalse(entity.getCatalogue());
        assertEquals("Motif de refus", entity.getMotifInvalidation());
        assertNotNull(entity.getDateValidation());
        verify(sujetProjetRepository).save(entity);
    }

    @Test
    void invaliderSujetAsAdmin_notPending_throwsIllegalArgument() {
        entity.setStatut(StatutSujet.INVALIDE);
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        var invalidation = new SujetInvalidationRequest("Motif");
        assertThrows(IllegalArgumentException.class,
                () -> service.invaliderSujetAsAdmin(1L, invalidation));
    }

    @Test
    void getSujetForAdmin_returnsResponse() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        SujetProjetResponse result = service.getSujetForAdmin(1L);

        assertEquals(response, result);
    }

    @Test
    void getSujetForAdmin_notFound_throwsIllegalArgument() {
        when(sujetProjetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getSujetForAdmin(99L));
    }

    @Test
    void modifierSujetAsAdmin_updatesAndSaves() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(sujetProjetRepository.save(entity)).thenReturn(entity);

        SujetProjetResponse result = service.modifierSujetAsAdmin(1L, request);

        assertEquals(response, result);
        verify(sujetProjetMapper).updateEntity(entity, request);
        verify(sujetProjetRepository).save(entity);
    }

    @Test
    void supprimerSujetAsAdmin_deletesSubject() {
        when(sujetProjetRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.supprimerSujetAsAdmin(1L);

        verify(sujetProjetRepository).delete(entity);
    }

    @Test
    void supprimerSujetAsAdmin_notFound_throwsIllegalArgument() {
        when(sujetProjetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.supprimerSujetAsAdmin(99L));
    }

    @Test
    void creerSujetAsAdmin_createsValidatedSubjectForAffiliatedEncadrant() {
        var equipe = tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche.builder()
                .id(1L).nom("AI Lab").build();
        User encadrant = User.builder().id(10L).nom("Ben Ali").prenom("Sami")
                .email("sami@esprit.tn").role(Role.ROLE_ENSEIGNANT).equipeRecherche(equipe).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(encadrant));
        when(sujetProjetMapper.toEntity(request, 10L)).thenReturn(entity);
        when(sujetProjetRepository.save(entity)).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCreateSujetProjetRequest createRequest = new AdminCreateSujetProjetRequest(10L, request);
        SujetProjetResponse result = service.creerSujetAsAdmin(createRequest);

        assertNotNull(result);
        assertEquals(StatutSujet.VALIDE, entity.getStatut());
        assertTrue(entity.getCatalogue());
        assertNotNull(entity.getDateValidation());
        assertNotNull(entity.getDateSoumission());
        verify(sujetProjetRepository).save(entity);
    }

    @Test
    void creerSujetAsAdmin_nonAffiliatedEncadrant_throwsIllegalArgument() {
        User encadrant = User.builder().id(10L).role(Role.ROLE_ENSEIGNANT).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(encadrant));

        AdminCreateSujetProjetRequest createRequest = new AdminCreateSujetProjetRequest(10L, request);

        assertThrows(IllegalArgumentException.class, () -> service.creerSujetAsAdmin(createRequest));
        verify(sujetProjetRepository, never()).save(any());
    }
}
