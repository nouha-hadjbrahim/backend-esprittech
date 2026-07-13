package tn.esprit.backend_esprittech.service.projetcatalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.CreateProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.RefuserProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCardResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCatalogueResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetDetailsResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ConflictException;
import tn.esprit.backend_esprittech.exception.ForbiddenException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.projetcatalogue.ProjetCatalogueMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetDomaineRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetPrerequisRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetTechnologieRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetReferenceService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetCatalogueServiceTest {

    @Mock private ProjetCatalogueRepository projetRepository;
    @Mock private ProjetCatalogueMapper mapper;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private EquipeRechercheRepository equipeRepository;
    @Mock private SujetDomaineRepository domaineRepository;
    @Mock private SujetTechnologieRepository technologieRepository;
    @Mock private SujetPrerequisRepository prerequisRepository;
    @Mock private LivrableCatalogueRepository livrableCatalogueRepository;
    @Mock private SujetReferenceService sujetReferenceService;

    @InjectMocks private ProjetCatalogueService service;

    private User enseignant;
    private User chef;
    private EquipeRecherche equipe;
    private ProjetCatalogue projet;
    private SujetDomaine domaine;
    private SujetTechnologie technologie;

    @BeforeEach
    void setUp() {
        chef = User.builder()
                .id(10L).nom("Chef").prenom("Equipe")
                .email("chef@esprit.tn").identifiant("ENS-010")
                .role(Role.ROLE_CHEF_EQUIPE).enabled(true)
                .build();

        equipe = EquipeRecherche.builder()
                .id(100L).nom("Equipe RDI").chef(chef).build();

        enseignant = User.builder()
                .id(1L).nom("Test").prenom("Enseignant")
                .email("test@esprit.tn").identifiant("ENS-001")
                .role(Role.ROLE_ENSEIGNANT).enabled(true)
                .equipeRecherche(equipe)
                .build();

        domaine = SujetDomaine.builder().id(1L).nom("IA").build();
        technologie = SujetTechnologie.builder().id(1L).nom("Spring Boot").build();

        projet = ProjetCatalogue.builder()
                .id(1L)
                .typeProjet(TypeProjet.RDI)
                .titre("Projet Test")
                .description("Description du projet test")
                .objectifs("Objectifs du projet")
                .dateDebut(LocalDate.of(2026, 1, 1))
                .dateFin(LocalDate.of(2026, 12, 31))
                .statut(StatutProjet.SOUMIS_EN_VALIDATION)
                .score(0)
                .encadrant(enseignant)
                .equipeRecherche(equipe)
                .domaines(new LinkedHashSet<>(List.of(domaine)))
                .technologies(new LinkedHashSet<>(List.of(technologie)))
                .prerequis(new LinkedHashSet<>())
                .dateCreation(LocalDateTime.now())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private CreateProjetRequest validRequest() {
        return new CreateProjetRequest(
                TypeProjet.RDI, "Projet Test", "Description longue du projet",
                "Objectifs", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                List.of(1L), List.of(1L), List.of(), null, null);
    }

    private ProjetCatalogueResponse dummyResponse() {
        return new ProjetCatalogueResponse(1L, TypeProjet.RDI, "Projet Test",
                "Description", "Objectifs", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                StatutProjet.SOUMIS_EN_VALIDATION, 0, 1L, "Enseignant Test", 100L, "Equipe RDI",
                null, null, null, LocalDateTime.now(), null,
                List.of("IA"), List.of("Spring Boot"), List.of());
    }

    private ProjetCardResponse dummyCard() {
        return new ProjetCardResponse(1L, TypeProjet.RDI, "Projet Test",
                "Description", StatutProjet.SOUMIS_EN_VALIDATION, 0, 1L, "Enseignant Test",
                100L, "Equipe RDI", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                LocalDateTime.now(), List.of("IA"), List.of("Spring Boot"), null);
    }

    private ProjetDetailsResponse dummyDetails() {
        return new ProjetDetailsResponse(1L, TypeProjet.RDI, "Projet Test",
                "Description", "Objectifs", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                StatutProjet.VALIDE, 0, 1L, "Enseignant Test", "test@esprit.tn",
                100L, "Equipe RDI", "Chef Equipe", null, null, null, LocalDateTime.now(), null,
                List.of("IA"), List.of("Spring Boot"), List.of(), null);
    }

    private void stubCurrentEnseignantManaged() {
        when(userService.getCurrentUser()).thenReturn(enseignant);
        when(userRepository.findById(enseignant.getId())).thenReturn(Optional.of(enseignant));
    }

    private void stubCurrentChef() {
        when(userService.getCurrentUser()).thenReturn(chef);
        when(userRepository.findById(chef.getId())).thenReturn(Optional.of(chef));
    }

    // ═════════════════════════════════════════════════════════════════════
    // creerProjet
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class CreerProjet {

        @Test
        void success() {
            stubCurrentEnseignantManaged();
            when(domaineRepository.findAllById(any())).thenReturn(List.of(domaine));
            when(technologieRepository.findAllById(any())).thenReturn(List.of(technologie));
            when(projetRepository.save(any(ProjetCatalogue.class))).thenReturn(projet);
            when(mapper.toResponse(projet)).thenReturn(dummyResponse());

            ProjetCatalogueResponse result = service.creerProjet(validRequest());

            assertNotNull(result);
            assertEquals("Projet Test", result.titre());
            assertEquals(StatutProjet.SOUMIS_EN_VALIDATION, result.statut());
            verify(projetRepository).save(any(ProjetCatalogue.class));
        }

        @Test
        void nonAffilie_throws403() {
            User nonAffilie = User.builder()
                    .id(2L).role(Role.ROLE_ENSEIGNANT).equipeRecherche(null).build();
            when(userService.getCurrentUser()).thenReturn(nonAffilie);
            when(userRepository.findById(2L)).thenReturn(Optional.of(nonAffilie));

            assertThrows(ForbiddenException.class, () -> service.creerProjet(validRequest()));
            verify(projetRepository, never()).save(any());
        }

        @Test
        void dateDebutApresDateFin_throwsBusiness() {
            stubCurrentEnseignantManaged();
            var request = new CreateProjetRequest(
                    TypeProjet.PFE, "Titre test", "Description longue",
                    "Objectifs", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1),
                    List.of(1L), List.of(1L), null, null, null);

            assertThrows(BusinessException.class, () -> service.creerProjet(request));
            verify(projetRepository, never()).save(any());
        }

        @Test
        void dateDebutEgaleDateFin_throwsBusiness() {
            stubCurrentEnseignantManaged();
            LocalDate same = LocalDate.of(2026, 6, 15);
            var request = new CreateProjetRequest(
                    TypeProjet.PFE, "Titre test", "Description longue",
                    "Objectifs", same, same, List.of(1L), List.of(1L), null, null, null);

            assertThrows(BusinessException.class, () -> service.creerProjet(request));
        }

        @Test
        void domaineIntrouvable_throwsNotFound() {
            stubCurrentEnseignantManaged();
            when(domaineRepository.findAllById(any())).thenReturn(List.of());

            assertThrows(ResourceNotFoundException.class, () -> service.creerProjet(validRequest()));
            verify(projetRepository, never()).save(any());
        }

        @Test
        void technologieIntrouvable_throwsNotFound() {
            stubCurrentEnseignantManaged();
            when(domaineRepository.findAllById(any())).thenReturn(List.of(domaine));
            when(technologieRepository.findAllById(any())).thenReturn(List.of());

            assertThrows(ResourceNotFoundException.class, () -> service.creerProjet(validRequest()));
            verify(projetRepository, never()).save(any());
        }

        @Test
        void nonEnseignant_throws403() {
            User etudiant = User.builder()
                    .id(3L).role(Role.ROLE_ETUDIANT).build();
            when(userService.getCurrentUser()).thenReturn(etudiant);

            assertThrows(ForbiddenException.class, () -> service.creerProjet(validRequest()));
        }

        @Test
        void prerequisVide_success() {
            stubCurrentEnseignantManaged();
            var request = new CreateProjetRequest(
                    TypeProjet.STAGE_INGENIEUR, "Stage", "Description longue du stage",
                    "Objectifs du stage", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1),
                    List.of(1L), List.of(1L), null, null, null);

            when(domaineRepository.findAllById(any())).thenReturn(List.of(domaine));
            when(technologieRepository.findAllById(any())).thenReturn(List.of(technologie));
            when(projetRepository.save(any(ProjetCatalogue.class))).thenReturn(projet);
            when(mapper.toResponse(projet)).thenReturn(dummyResponse());

            ProjetCatalogueResponse result = service.creerProjet(request);
            assertNotNull(result);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // mesProjets
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class MesProjets {

        @Test
        void success_returnsCards() {
            when(userService.getCurrentUser()).thenReturn(enseignant);
            when(projetRepository.findByEncadrantIdOrderByDateCreationDesc(1L))
                    .thenReturn(List.of(projet));
            when(mapper.toCard(projet)).thenReturn(dummyCard());

            List<ProjetCardResponse> result = service.mesProjets();

            assertEquals(1, result.size());
            assertEquals("Projet Test", result.get(0).titre());
        }

        @Test
        void empty_returnsEmptyList() {
            when(userService.getCurrentUser()).thenReturn(enseignant);
            when(projetRepository.findByEncadrantIdOrderByDateCreationDesc(1L))
                    .thenReturn(List.of());

            List<ProjetCardResponse> result = service.mesProjets();
            assertTrue(result.isEmpty());
        }

        @Test
        void nonEnseignant_throws403() {
            when(userService.getCurrentUser()).thenReturn(chef);
            assertThrows(ForbiddenException.class, () -> service.mesProjets());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // projetsAValider
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class ProjetsAValider {

        @Test
        void success_returnsCards() {
            stubCurrentChef();
            when(equipeRepository.findByChefId(chef.getId())).thenReturn(Optional.of(equipe));
            when(projetRepository.findByEquipeRechercheIdAndStatutOrderByDateCreationDesc(
                    equipe.getId(), StatutProjet.SOUMIS_EN_VALIDATION))
                    .thenReturn(List.of(projet));
            when(mapper.toCard(projet)).thenReturn(dummyCard());

            List<ProjetCardResponse> result = service.projetsAValider();

            assertEquals(1, result.size());
        }

        @Test
        void chefSansEquipe_returnsEmpty() {
            stubCurrentChef();
            when(equipeRepository.findByChefId(chef.getId())).thenReturn(Optional.empty());

            List<ProjetCardResponse> result = service.projetsAValider();
            assertTrue(result.isEmpty());
        }

        @Test
        void nonChef_throws403() {
            when(userService.getCurrentUser()).thenReturn(enseignant);
            assertThrows(ForbiddenException.class, () -> service.projetsAValider());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // valider
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class Valider {

        @Test
        void success() {
            stubCurrentChef();
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
            when(projetRepository.save(any(ProjetCatalogue.class))).thenReturn(projet);
            when(mapper.toResponse(any())).thenReturn(dummyResponse());

            ProjetCatalogueResponse result = service.valider(1L);

            assertNotNull(result);
            verify(projetRepository).save(argThat(p ->
                    p.getStatut() == StatutProjet.VALIDE && p.getChefValidateur() != null));
        }

        @Test
        void projetInexistant_throwsNotFound() {
            stubCurrentChef();
            when(projetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.valider(99L));
        }

        @Test
        void chefAutreEquipe_throws403() {
            User autreChef = User.builder()
                    .id(20L).role(Role.ROLE_CHEF_EQUIPE).build();
            when(userService.getCurrentUser()).thenReturn(autreChef);
            when(userRepository.findById(20L)).thenReturn(Optional.of(autreChef));
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ForbiddenException.class, () -> service.valider(1L));
        }

        @Test
        void projetDejaValide_throwsConflict() {
            stubCurrentChef();
            projet.setStatut(StatutProjet.VALIDE);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ConflictException.class, () -> service.valider(1L));
        }

        @Test
        void projetInvalide_throwsConflict() {
            stubCurrentChef();
            projet.setStatut(StatutProjet.INVALIDE);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ConflictException.class, () -> service.valider(1L));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // refuser
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class Refuser {

        @Test
        void success() {
            stubCurrentChef();
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
            when(projetRepository.save(any(ProjetCatalogue.class))).thenReturn(projet);
            when(mapper.toResponse(any())).thenReturn(dummyResponse());

            var request = new RefuserProjetRequest("Projet incomplet");
            ProjetCatalogueResponse result = service.refuser(1L, request);

            assertNotNull(result);
            verify(projetRepository).save(argThat(p ->
                    p.getStatut() == StatutProjet.INVALIDE
                            && "Projet incomplet".equals(p.getMotifRefus())));
        }

        @Test
        void projetInexistant_throwsNotFound() {
            stubCurrentChef();
            when(projetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.refuser(99L, new RefuserProjetRequest("motif")));
        }

        @Test
        void chefAutreEquipe_throws403() {
            User autreChef = User.builder()
                    .id(20L).role(Role.ROLE_CHEF_EQUIPE).build();
            when(userService.getCurrentUser()).thenReturn(autreChef);
            when(userRepository.findById(20L)).thenReturn(Optional.of(autreChef));
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ForbiddenException.class,
                    () -> service.refuser(1L, new RefuserProjetRequest("motif")));
        }

        @Test
        void projetDejaValide_throwsConflict() {
            stubCurrentChef();
            projet.setStatut(StatutProjet.VALIDE);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ConflictException.class,
                    () -> service.refuser(1L, new RefuserProjetRequest("motif")));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // catalogue
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class CatalogueTests {

        @Test
        void sansFiltres_retourneTousLesProjets() {
            when(projetRepository.rechercherCatalogue(
                    eq(StatutProjet.STATUTS_CATALOGUE), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of(projet));
            when(mapper.toCard(projet)).thenReturn(dummyCard());

            List<ProjetCardResponse> result = service.catalogue(null, null, null, null);
            assertEquals(1, result.size());
        }

        @Test
        void avecRecherche_trimmeLeTerme() {
            when(projetRepository.rechercherCatalogue(
                    eq(StatutProjet.STATUTS_CATALOGUE), isNull(), isNull(), isNull(), eq("spring")))
                    .thenReturn(List.of(projet));
            when(mapper.toCard(projet)).thenReturn(dummyCard());

            List<ProjetCardResponse> result = service.catalogue(null, null, null, "  spring  ");
            assertEquals(1, result.size());
        }

        @Test
        void rechercheVide_passeNull() {
            when(projetRepository.rechercherCatalogue(
                    eq(StatutProjet.STATUTS_CATALOGUE), isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of());

            List<ProjetCardResponse> result = service.catalogue(null, null, null, "   ");
            assertTrue(result.isEmpty());
        }

        @Test
        void avecFiltreType() {
            when(projetRepository.rechercherCatalogue(
                    eq(StatutProjet.STATUTS_CATALOGUE), eq(TypeProjet.PFE), isNull(), isNull(), isNull()))
                    .thenReturn(List.of());

            List<ProjetCardResponse> result = service.catalogue(TypeProjet.PFE, null, null, null);
            assertTrue(result.isEmpty());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // detailsCatalogue
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class DetailsCatalogue {

        @Test
        void projetValide_success() {
            projet.setStatut(StatutProjet.VALIDE);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
            when(mapper.toDetails(projet)).thenReturn(dummyDetails());

            ProjetDetailsResponse result = service.detailsCatalogue(1L);
            assertNotNull(result);
            assertEquals("Projet Test", result.titre());
        }

        @Test
        void projetSoumis_throwsNotFound() {
            projet.setStatut(StatutProjet.SOUMIS_EN_VALIDATION);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ResourceNotFoundException.class, () -> service.detailsCatalogue(1L));
        }

        @Test
        void projetInvalide_throwsNotFound() {
            projet.setStatut(StatutProjet.INVALIDE);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ResourceNotFoundException.class, () -> service.detailsCatalogue(1L));
        }

        @Test
        void projetInexistant_throwsNotFound() {
            when(projetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.detailsCatalogue(99L));
        }

        @Test
        void projetIndustrialise_success() {
            projet.setStatut(StatutProjet.INDUSTRIALISE_DSI);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
            when(mapper.toDetails(projet)).thenReturn(dummyDetails());

            ProjetDetailsResponse result = service.detailsCatalogue(1L);
            assertNotNull(result);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // detailsProjet (encadrant / chef / admin)
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class DetailsProjet {

        @Test
        void encadrant_success() {
            when(userService.getCurrentUser()).thenReturn(enseignant);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
            when(mapper.toDetails(projet)).thenReturn(dummyDetails());

            ProjetDetailsResponse result = service.detailsProjet(1L);
            assertNotNull(result);
        }

        @Test
        void chefEquipe_success() {
            when(userService.getCurrentUser()).thenReturn(chef);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
            when(mapper.toDetails(projet)).thenReturn(dummyDetails());

            ProjetDetailsResponse result = service.detailsProjet(1L);
            assertNotNull(result);
        }

        @Test
        void admin_success() {
            User admin = User.builder()
                    .id(99L).role(Role.ROLE_ADMIN).build();
            when(userService.getCurrentUser()).thenReturn(admin);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));
            when(mapper.toDetails(projet)).thenReturn(dummyDetails());

            ProjetDetailsResponse result = service.detailsProjet(1L);
            assertNotNull(result);
        }

        @Test
        void autreUtilisateur_throws403() {
            User autre = User.builder()
                    .id(50L).role(Role.ROLE_ETUDIANT).build();
            when(userService.getCurrentUser()).thenReturn(autre);
            when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

            assertThrows(ForbiddenException.class, () -> service.detailsProjet(1L));
        }

        @Test
        void projetInexistant_throwsNotFound() {
            when(userService.getCurrentUser()).thenReturn(enseignant);
            when(projetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.detailsProjet(99L));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Références
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    class References {

        @Test
        void getDomaines_returnsList() {
            var dom = SujetDomaine.builder().id(1L).nom("IA").dateCreation(LocalDateTime.now()).build();
            when(domaineRepository.findAllByOrderByNomAsc()).thenReturn(List.of(dom));

            List<ReferenceItemResponse> result = service.getDomaines();
            assertEquals(1, result.size());
            assertEquals("IA", result.get(0).nom());
        }

        @Test
        void getTechnologies_returnsList() {
            var tech = SujetTechnologie.builder().id(1L).nom("Spring").dateCreation(LocalDateTime.now()).build();
            when(technologieRepository.findAllByOrderByNomAsc()).thenReturn(List.of(tech));

            List<ReferenceItemResponse> result = service.getTechnologies();
            assertEquals(1, result.size());
            assertEquals("Spring", result.get(0).nom());
        }

        @Test
        void getPrerequis_returnsList() {
            var prereq = SujetPrerequis.builder().id(1L).nom("Java").dateCreation(LocalDateTime.now()).build();
            when(prerequisRepository.findAllByOrderByNomAsc()).thenReturn(List.of(prereq));

            List<ReferenceItemResponse> result = service.getPrerequis();
            assertEquals(1, result.size());
            assertEquals("Java", result.get(0).nom());
        }

    @Test
    void getDomaines_empty() {
        when(domaineRepository.findAllByOrderByNomAsc()).thenReturn(List.of());

        List<ReferenceItemResponse> result = service.getDomaines();
        assertTrue(result.isEmpty());
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsProjetFields() {
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

        var result = service.getOldValues(1L);

        assertEquals(projet.getStatut(), result.get("statut"));
    }

    // ---- getTitreById ----

    @Test
    void getTitreById_found_returnsTitre() {
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet));

        assertEquals("Projet Test", service.getTitreById(1L));
    }

    @Test
    void getTitreById_notFound_returnsInconnu() {
        when(projetRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("Inconnu", service.getTitreById(99L));
    }
}
}
