package tn.esprit.backend_esprittech.mapper.projetcatalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCardResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCatalogueResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetDetailsResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjetCatalogueMapperTest {

    private final ProjetCatalogueMapper mapper = new ProjetCatalogueMapper();

    private User encadrant;
    private User chef;
    private EquipeRecherche equipe;
    private ProjetCatalogue projet;

    @BeforeEach
    void setUp() {
        encadrant = User.builder()
                .id(1L).nom("Dupont").prenom("Jean")
                .email("jean@esprit.tn").role(Role.ROLE_ENSEIGNANT).build();

        chef = User.builder()
                .id(10L).nom("Martin").prenom("Alice")
                .email("alice@esprit.tn").role(Role.ROLE_CHEF_EQUIPE).build();

        equipe = EquipeRecherche.builder()
                .id(100L).nom("Equipe IA").chef(chef).build();

        var domaine = SujetDomaine.builder().id(1L).nom("Intelligence Artificielle").build();
        var tech = SujetTechnologie.builder().id(1L).nom("TensorFlow").build();
        var prereq = SujetPrerequis.builder().id(1L).nom("Python").build();

        projet = ProjetCatalogue.builder()
                .id(1L)
                .typeProjet(TypeProjet.RDI)
                .titre("Projet ML")
                .description("Description du projet ML")
                .objectifs("Objectifs ML")
                .dateDebut(LocalDate.of(2026, 3, 1))
                .dateFin(LocalDate.of(2026, 9, 30))
                .statut(StatutProjet.VALIDE)
                .score(85)
                .encadrant(encadrant)
                .equipeRecherche(equipe)
                .chefValidateur(chef)
                .motifRefus(null)
                .dateCreation(LocalDateTime.of(2026, 2, 1, 10, 0))
                .dateValidation(LocalDateTime.of(2026, 2, 15, 14, 30))
                .domaines(new LinkedHashSet<>(List.of(domaine)))
                .technologies(new LinkedHashSet<>(List.of(tech)))
                .prerequis(new LinkedHashSet<>(List.of(prereq)))
                .build();
    }

    // ── toCard ───────────────────────────────────────────────────────

    @Test
    void toCard_mapsAllFields() {
        ProjetCardResponse card = mapper.toCard(projet);

        assertEquals(1L, card.id());
        assertEquals(TypeProjet.RDI, card.typeProjet());
        assertEquals("Projet ML", card.titre());
        assertEquals("Description du projet ML", card.description());
        assertEquals(StatutProjet.VALIDE, card.statut());
        assertEquals(85, card.score());
        assertEquals(1L, card.encadrantId());
        assertEquals("Jean Dupont", card.encadrantNom());
        assertEquals(100L, card.equipeId());
        assertEquals("Equipe IA", card.equipeNom());
        assertEquals(LocalDate.of(2026, 3, 1), card.dateDebut());
        assertEquals(LocalDate.of(2026, 9, 30), card.dateFin());
        assertEquals(LocalDateTime.of(2026, 2, 1, 10, 0), card.dateCreation());
        assertEquals(List.of("Intelligence Artificielle"), card.domaines());
        assertEquals(List.of("TensorFlow"), card.technologies());
    }

    @Test
    void toCard_nullEncadrant_returnsNulls() {
        projet.setEncadrant(null);
        projet.setEquipeRecherche(null);

        ProjetCardResponse card = mapper.toCard(projet);

        assertNull(card.encadrantId());
        assertNull(card.encadrantNom());
        assertNull(card.equipeId());
        assertNull(card.equipeNom());
    }

    // ── toResponse ───────────────────────────────────────────────────

    @Test
    void toResponse_mapsAllFields() {
        ProjetCatalogueResponse resp = mapper.toResponse(projet);

        assertEquals(1L, resp.id());
        assertEquals(TypeProjet.RDI, resp.typeProjet());
        assertEquals("Projet ML", resp.titre());
        assertEquals("Description du projet ML", resp.description());
        assertEquals("Objectifs ML", resp.objectifs());
        assertEquals(LocalDate.of(2026, 3, 1), resp.dateDebut());
        assertEquals(LocalDate.of(2026, 9, 30), resp.dateFin());
        assertEquals(StatutProjet.VALIDE, resp.statut());
        assertEquals(85, resp.score());
        assertEquals(1L, resp.encadrantId());
        assertEquals("Jean Dupont", resp.encadrantNom());
        assertEquals(100L, resp.equipeId());
        assertEquals("Equipe IA", resp.equipeNom());
        assertEquals(10L, resp.chefValidateurId());
        assertEquals("Alice Martin", resp.chefValidateurNom());
        assertNull(resp.motifRefus());
        assertEquals(LocalDateTime.of(2026, 2, 1, 10, 0), resp.dateCreation());
        assertEquals(LocalDateTime.of(2026, 2, 15, 14, 30), resp.dateValidation());
        assertEquals(List.of("Intelligence Artificielle"), resp.domaines());
        assertEquals(List.of("TensorFlow"), resp.technologies());
        assertEquals(List.of("Python"), resp.prerequis());
    }

    @Test
    void toResponse_nullChefValidateur() {
        projet.setChefValidateur(null);

        ProjetCatalogueResponse resp = mapper.toResponse(projet);

        assertNull(resp.chefValidateurId());
        assertNull(resp.chefValidateurNom());
    }

    @Test
    void toResponse_withMotifRefus() {
        projet.setStatut(StatutProjet.INVALIDE);
        projet.setMotifRefus("Projet incomplet");

        ProjetCatalogueResponse resp = mapper.toResponse(projet);

        assertEquals(StatutProjet.INVALIDE, resp.statut());
        assertEquals("Projet incomplet", resp.motifRefus());
    }

    // ── toDetails ────────────────────────────────────────────────────

    @Test
    void toDetails_mapsAllFieldsIncludingEmail() {
        ProjetDetailsResponse details = mapper.toDetails(projet);

        assertEquals(1L, details.id());
        assertEquals("Projet ML", details.titre());
        assertEquals("jean@esprit.tn", details.encadrantEmail());
        assertEquals("Jean Dupont", details.encadrantNom());
        assertEquals(100L, details.equipeId());
        assertEquals(List.of("Intelligence Artificielle"), details.domaines());
        assertEquals(List.of("TensorFlow"), details.technologies());
        assertEquals(List.of("Python"), details.prerequis());
    }

    @Test
    void toDetails_nullEncadrant_emailIsNull() {
        projet.setEncadrant(null);

        ProjetDetailsResponse details = mapper.toDetails(projet);

        assertNull(details.encadrantId());
        assertNull(details.encadrantNom());
        assertNull(details.encadrantEmail());
    }

    @Test
    void toDetails_emptyCollections() {
        projet.setDomaines(new LinkedHashSet<>());
        projet.setTechnologies(new LinkedHashSet<>());
        projet.setPrerequis(new LinkedHashSet<>());

        ProjetDetailsResponse details = mapper.toDetails(projet);

        assertTrue(details.domaines().isEmpty());
        assertTrue(details.technologies().isEmpty());
        assertTrue(details.prerequis().isEmpty());
    }

    @Test
    void toCard_sortsDomaines() {
        var d1 = SujetDomaine.builder().id(1L).nom("Zeta").build();
        var d2 = SujetDomaine.builder().id(2L).nom("Alpha").build();
        projet.setDomaines(new LinkedHashSet<>(List.of(d1, d2)));

        ProjetCardResponse card = mapper.toCard(projet);

        assertEquals(List.of("Alpha", "Zeta"), card.domaines());
    }

    @Test
    void toCard_sortsTechnologies() {
        var t1 = SujetTechnologie.builder().id(1L).nom("Vue").build();
        var t2 = SujetTechnologie.builder().id(2L).nom("Angular").build();
        projet.setTechnologies(new LinkedHashSet<>(List.of(t1, t2)));

        ProjetCardResponse card = mapper.toCard(projet);

        assertEquals(List.of("Angular", "Vue"), card.technologies());
    }
}
