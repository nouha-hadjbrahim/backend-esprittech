package tn.esprit.backend_esprittech.mapper;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.request.CreateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.response.ResearchTeamResponse;
import tn.esprit.backend_esprittech.entity.ResearchTeam;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResearchTeamMapperTest {

    private final ResearchTeamMapper mapper = new ResearchTeamMapper();

    private User buildTeacher(Long id, String nom, String prenom) {
        return User.builder()
                .id(id)
                .nom(nom)
                .prenom(prenom)
                .email(nom.toLowerCase() + "@esprit.tn")
                .identifiant("ENS-" + id)
                .password("encoded")
                .role(Role.ROLE_ENSEIGNANT)
                .typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                .enabled(true)
                .build();
    }

    // =========================
    // toEntity
    // =========================

    @Test
    void toEntity_shouldReturnNull_whenRequestIsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_shouldMapCorrectly() {
        CreateResearchTeamRequest req =
                new CreateResearchTeamRequest("AI", "DS", "desc", 1L, List.of());

        ResearchTeam result = mapper.toEntity(req);

        assertEquals("AI", result.getNom());
        assertEquals("DS", result.getDomaineRecherche());
        assertEquals("desc", result.getDescription());
    }

    // =========================
    // updateEntity
    // =========================

    @Test
    void updateEntity_shouldDoNothing_whenRequestIsNull() {
        ResearchTeam team = new ResearchTeam();
        mapper.updateEntity(null, team);

        assertNotNull(team);
    }

    @Test
    void updateEntity_shouldDoNothing_whenTeamIsNull() {
        mapper.updateEntity(
                new UpdateResearchTeamRequest("A", "B", "C", null, null),
                null
        );
    }

    // =========================
    // UPDATE ONLY NOM
    // =========================
    @Test
    void updateEntity_shouldUpdateOnlyNom() {

        ResearchTeam team = new ResearchTeam();
        team.setNom("Old");
        team.setDomaineRecherche("Old");
        team.setDescription("Old");

        UpdateResearchTeamRequest req =
                new UpdateResearchTeamRequest("NEW_NAME", null, null, null, null);

        mapper.updateEntity(req, team);

        assertEquals("NEW_NAME", team.getNom());
        assertEquals("Old", team.getDomaineRecherche());
        assertEquals("Old", team.getDescription());
    }

    // =========================
    // UPDATE ONLY DOMAINE
    // =========================
    @Test
    void updateEntity_shouldUpdateOnlyDomaine() {

        ResearchTeam team = new ResearchTeam();
        team.setNom("Old");
        team.setDomaineRecherche("Old");
        team.setDescription("Old");

        UpdateResearchTeamRequest req =
                new UpdateResearchTeamRequest(null, "NEW_DOMAIN", null, null, null);

        mapper.updateEntity(req, team);

        assertEquals("NEW_DOMAIN", team.getDomaineRecherche());
        assertEquals("Old", team.getNom());
        assertEquals("Old", team.getDescription());
    }

    // =========================
    // UPDATE ONLY DESCRIPTION
    // =========================
    @Test
    void updateEntity_shouldUpdateOnlyDescription() {

        ResearchTeam team = new ResearchTeam();
        team.setNom("Old");
        team.setDomaineRecherche("Old");
        team.setDescription("Old");

        UpdateResearchTeamRequest req =
                new UpdateResearchTeamRequest(null, null, "NEW_DESC", null, null);

        mapper.updateEntity(req, team);

        assertEquals("NEW_DESC", team.getDescription());
        assertEquals("Old", team.getNom());
        assertEquals("Old", team.getDomaineRecherche());
    }

    @Test
    void updateEntity_shouldUpdateOnlyNonNullFields() {
        ResearchTeam team = new ResearchTeam();
        team.setNom("Old");
        team.setDomaineRecherche("Old domain");
        team.setDescription("Old desc");

        UpdateResearchTeamRequest req =
                new UpdateResearchTeamRequest("New", null, "New desc", null, null);

        mapper.updateEntity(req, team);

        assertEquals("New", team.getNom());
        assertEquals("Old domain", team.getDomaineRecherche());
        assertEquals("New desc", team.getDescription());
    }

    @Test
    void updateEntity_shouldUpdateAllFields() {
        ResearchTeam team = new ResearchTeam();

        UpdateResearchTeamRequest req =
                new UpdateResearchTeamRequest("A", "B", "C", null, null);

        mapper.updateEntity(req, team);

        assertEquals("A", team.getNom());
        assertEquals("B", team.getDomaineRecherche());
        assertEquals("C", team.getDescription());
    }

    @Test
    void toResponse_mapsAllFields() {
        User leader = buildTeacher(1L, "Leader", "One");
        User member = buildTeacher(2L, "Member", "One");
        ResearchTeam team = ResearchTeam.builder()
                .id(10L)
                .nom("AI")
                .domaineRecherche("Intelligence artificielle")
                .description("Research team")
                .leader(leader)
                .members(new ArrayList<>(List.of(member)))
                .build();

        ResearchTeamResponse response = mapper.toResponse(team);

        assertEquals(10L, response.id());
        assertEquals("AI", response.nom());
        assertEquals("Intelligence artificielle", response.domaineRecherche());
        assertEquals("Research team", response.description());
        assertEquals(1L, response.chef().id());
        assertEquals("Leader One", response.chef().name());
        assertEquals(1, response.members().size());
        assertEquals("Member One", response.members().get(0).name());
    }

    @Test
    void toResponse_nullTeam_returnsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toUserResponse_nullUser_returnsNull() {
        assertNull(mapper.toUserResponse(null));
    }
}