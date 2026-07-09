package tn.esprit.backend_esprittech.controller.projetcatalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.CreateProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.RefuserProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCardResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCatalogueResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetDetailsResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;
import tn.esprit.backend_esprittech.service.projetcatalogue.ProjetCatalogueService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetCatalogueControllerTest {

    @Mock private ProjetCatalogueService projetService;
    @InjectMocks private ProjetCatalogueController controller;

    private ProjetCatalogueResponse catalogueResponse;
    private ProjetCardResponse cardResponse;
    private ProjetDetailsResponse detailsResponse;

    @BeforeEach
    void setUp() {
        catalogueResponse = new ProjetCatalogueResponse(
                1L, TypeProjet.RDI, "Projet IA", "Desc", "Obj",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                StatutProjet.SOUMIS_EN_VALIDATION, 0, 1L, "Enseignant Test",
                100L, "Equipe RDI", null, null, null,
                LocalDateTime.now(), null,
                List.of("IA"), List.of("Spring"), List.of());

        cardResponse = new ProjetCardResponse(
                1L, TypeProjet.RDI, "Projet IA", "Desc",
                StatutProjet.SOUMIS_EN_VALIDATION, 0, 1L, "Enseignant Test",
                100L, "Equipe RDI", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                LocalDateTime.now(), List.of("IA"), List.of("Spring"), null);

        detailsResponse = new ProjetDetailsResponse(
                1L, TypeProjet.RDI, "Projet IA", "Desc", "Obj",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                StatutProjet.VALIDE, 0, 1L, "Enseignant Test", "test@esprit.tn",
                100L, "Equipe RDI", "Chef Equipe", null, null, null,
                LocalDateTime.now(), null,
                List.of("IA"), List.of("Spring"), List.of(), null);
    }

    // ── Enseignant ────────────────────────────────────────────────────

    @Nested
    class EnseignantEndpoints {

        @Test
        void creerProjet_returns201() {
            var request = new CreateProjetRequest(
                    TypeProjet.RDI, "Projet IA", "Description longue",
                    "Objectifs", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    List.of(1L), List.of(1L), List.of(), null, null);
            when(projetService.creerProjet(request)).thenReturn(catalogueResponse);

            ResponseEntity<ProjetCatalogueResponse> result = controller.creerProjet(request);

            assertEquals(HttpStatus.CREATED, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals("Projet IA", result.getBody().titre());
        }

        @Test
        void mesProjets_returns200() {
            when(projetService.mesProjets()).thenReturn(List.of(cardResponse));

            ResponseEntity<List<ProjetCardResponse>> result = controller.mesProjets();

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().size());
        }

        @Test
        void mesProjets_empty_returns200() {
            when(projetService.mesProjets()).thenReturn(List.of());

            ResponseEntity<List<ProjetCardResponse>> result = controller.mesProjets();

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertTrue(result.getBody().isEmpty());
        }

        @Test
        void getDomaines_returns200() {
            var ref = new ReferenceItemResponse(1L, "IA", LocalDateTime.now());
            when(projetService.getDomaines()).thenReturn(List.of(ref));

            ResponseEntity<List<ReferenceItemResponse>> result = controller.getDomaines();

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().size());
            assertEquals("IA", result.getBody().get(0).nom());
        }

        @Test
        void getTechnologies_returns200() {
            var ref = new ReferenceItemResponse(1L, "Spring", LocalDateTime.now());
            when(projetService.getTechnologies()).thenReturn(List.of(ref));

            ResponseEntity<List<ReferenceItemResponse>> result = controller.getTechnologies();

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().size());
        }

        @Test
        void getPrerequis_returns200() {
            when(projetService.getPrerequis()).thenReturn(List.of());

            ResponseEntity<List<ReferenceItemResponse>> result = controller.getPrerequis();

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertTrue(result.getBody().isEmpty());
        }
    }

    // ── Chef d'équipe ────────────────────────────────────────────────

    @Nested
    class ChefEndpoints {

        @Test
        void projetsAValider_returns200() {
            when(projetService.projetsAValider()).thenReturn(List.of(cardResponse));

            ResponseEntity<List<ProjetCardResponse>> result = controller.projetsAValider();

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().size());
        }

        @Test
        void valider_returns200() {
            when(projetService.valider(1L)).thenReturn(catalogueResponse);

            ResponseEntity<ProjetCatalogueResponse> result = controller.valider(1L);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
        }

        @Test
        void refuser_returns200() {
            var request = new RefuserProjetRequest("Incomplet, manque les objectifs");
            when(projetService.refuser(1L, request)).thenReturn(catalogueResponse);

            ResponseEntity<ProjetCatalogueResponse> result = controller.refuser(1L, request);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
        }
    }

    // ── Détails ──────────────────────────────────────────────────────

    @Nested
    class DetailsEndpoints {

        @Test
        void detailsProjet_returns200() {
            when(projetService.detailsProjet(1L)).thenReturn(detailsResponse);

            ResponseEntity<ProjetDetailsResponse> result = controller.detailsProjet(1L);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
            assertEquals("test@esprit.tn", result.getBody().encadrantEmail());
        }

        @Test
        void catalogue_returns200() {
            when(projetService.catalogue(null, null, null, null)).thenReturn(List.of(cardResponse));

            ResponseEntity<List<ProjetCardResponse>> result =
                    controller.catalogue(null, null, null, null);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().size());
        }

        @Test
        void catalogue_avecFiltres_returns200() {
            when(projetService.catalogue(TypeProjet.PFE, 1L, 2026, "spring"))
                    .thenReturn(List.of());

            ResponseEntity<List<ProjetCardResponse>> result =
                    controller.catalogue(TypeProjet.PFE, 1L, 2026, "spring");

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertTrue(result.getBody().isEmpty());
        }

        @Test
        void detailsCatalogue_returns200() {
            when(projetService.detailsCatalogue(1L)).thenReturn(detailsResponse);

            ResponseEntity<ProjetDetailsResponse> result = controller.detailsCatalogue(1L);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals("Projet IA", result.getBody().titre());
        }
    }
}
