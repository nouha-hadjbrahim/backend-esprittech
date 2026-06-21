package tn.esprit.backend_esprittech.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchTeamTest {

    private ResearchTeam team;
    private User user;

    @BeforeEach
    void setUp() {
        team = new ResearchTeam();
        user = new User();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Constructors & Builder
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void noArgsConstructor_initializesEmptyMembersList() {
        ResearchTeam t = new ResearchTeam();
        assertThat(t.getMembers()).isNotNull().isEmpty();
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        User leader = new User();
        ArrayList<User> members = new ArrayList<>();
        ResearchTeam t = new ResearchTeam(1L, "Team A", "IA", "desc", leader, members);

        assertThat(t.getId()).isEqualTo(1L);
        assertThat(t.getNom()).isEqualTo("Team A");
        assertThat(t.getDomaineRecherche()).isEqualTo("IA");
        assertThat(t.getDescription()).isEqualTo("desc");
        assertThat(t.getLeader()).isSameAs(leader);
        assertThat(t.getMembers()).isSameAs(members);
    }

    @Test
    void builder_setsAllFields() {
        User leader = new User();
        ResearchTeam t = ResearchTeam.builder()
                .id(2L)
                .nom("Bio Team")
                .domaineRecherche("Biologie")
                .description("Research in bio")
                .leader(leader)
                .build();

        assertThat(t.getId()).isEqualTo(2L);
        assertThat(t.getNom()).isEqualTo("Bio Team");
        assertThat(t.getDomaineRecherche()).isEqualTo("Biologie");
        assertThat(t.getDescription()).isEqualTo("Research in bio");
        assertThat(t.getLeader()).isSameAs(leader);
        assertThat(t.getMembers()).isNotNull().isEmpty(); // @Builder.Default
    }

    @Test
    void settersAndGetters_workCorrectly() {
        User leader = new User();
        team.setId(5L);
        team.setNom("Test");
        team.setDomaineRecherche("Math");
        team.setDescription("Mathematics research");
        team.setLeader(leader);

        assertThat(team.getId()).isEqualTo(5L);
        assertThat(team.getNom()).isEqualTo("Test");
        assertThat(team.getDomaineRecherche()).isEqualTo("Math");
        assertThat(team.getDescription()).isEqualTo("Mathematics research");
        assertThat(team.getLeader()).isSameAs(leader);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  addMember()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void addMember_addsUserToList_andSetsResearchTeam() {
        team.addMember(user);

        assertThat(team.getMembers()).containsExactly(user);
        assertThat(user.getResearchTeam()).isSameAs(team);
    }

    @Test
    void addMember_null_doesNothing() {
        team.addMember(null);

        assertThat(team.getMembers()).isEmpty();
    }

    @Test
    void addMember_multipleUsers_allAdded() {
        User u1 = new User();
        User u2 = new User();

        team.addMember(u1);
        team.addMember(u2);

        assertThat(team.getMembers()).containsExactly(u1, u2);
        assertThat(u1.getResearchTeam()).isSameAs(team);
        assertThat(u2.getResearchTeam()).isSameAs(team);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  removeMember()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void removeMember_removesUserFromList_andClearsResearchTeam() {
        team.addMember(user);
        assertThat(team.getMembers()).hasSize(1);

        team.removeMember(user);

        assertThat(team.getMembers()).isEmpty();
        assertThat(user.getResearchTeam()).isNull();
    }

    @Test
    void removeMember_null_doesNothing() {
        team.addMember(user);

        team.removeMember(null);

        assertThat(team.getMembers()).containsExactly(user); // unchanged
    }

    @Test
    void removeMember_userBelongingToDifferentTeam_doesNotClearResearchTeam() {
        ResearchTeam otherTeam = new ResearchTeam();
        // manually put user in the list but set their researchTeam to another team
        team.getMembers().add(user);
        user.setResearchTeam(otherTeam); // user.researchTeam != team

        team.removeMember(user);

        // user removed from list, but researchTeam NOT nulled (it was a different team)
        assertThat(team.getMembers()).isEmpty();
        assertThat(user.getResearchTeam()).isSameAs(otherTeam);
    }

    @Test
    void removeMember_userNotInList_doesNotThrow() {
        User other = new User();
        team.addMember(user);

        team.removeMember(other); // other is not in the list

        assertThat(team.getMembers()).containsExactly(user); // user still there
    }
}