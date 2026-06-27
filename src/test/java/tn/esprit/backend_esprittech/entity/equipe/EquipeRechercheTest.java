package tn.esprit.backend_esprittech.entity.equipe;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.equipe.StatutEquipe;

class EquipeRechercheTest {

    // ---------------- onCreate ----------------

    @Test
    void onCreate_createdAtNull_setsValue() {
        EquipeRecherche equipe = new EquipeRecherche();

        equipe.setCreatedAt(null);
        equipe.onCreate();

        assertNotNull(equipe.getCreatedAt());
    }

    @Test
    void onCreate_createdAtAlreadySet_keepsValue() {
        EquipeRecherche equipe = new EquipeRecherche();

        Instant fixed = Instant.parse("2020-01-01T00:00:00Z");
        equipe.setCreatedAt(fixed);

        equipe.onCreate();

        assertEquals(fixed, equipe.getCreatedAt());
    }

    // ---------------- addMember ----------------

    @Test
    void addMember_nullUser_doesNothing() {
        EquipeRecherche equipe = new EquipeRecherche();

        equipe.addMember(null);

        assertTrue(equipe.getMembers().isEmpty());
    }

    @Test
    void addMember_validUser_addsAndSetsRelation() {
        EquipeRecherche equipe = new EquipeRecherche();
        User user = new User();

        equipe.addMember(user);

        assertEquals(1, equipe.getMembers().size());
        assertEquals(equipe, user.getEquipeRecherche());
        assertTrue(equipe.getMembers().contains(user));
    }

    // ---------------- removeMember ----------------

    @Test
    void removeMember_nullUser_doesNothing() {
        EquipeRecherche equipe = new EquipeRecherche();

        equipe.removeMember(null);

        assertTrue(equipe.getMembers().isEmpty());
    }

    @Test
    void removeMember_existingUser_removesAndUnlinks() {
        EquipeRecherche equipe = new EquipeRecherche();
        User user = new User();

        equipe.addMember(user);
        assertEquals(1, equipe.getMembers().size());

        equipe.removeMember(user);

        assertTrue(equipe.getMembers().isEmpty());
        assertNull(user.getEquipeRecherche());
    }

    // ---------------- builder behavior ----------------

    @Test
    void builder_defaultMembers_initialized() {
        EquipeRecherche equipe = EquipeRecherche.builder()
                .nom("AI Team")
                .description("Research team")
                .build();

        assertNotNull(equipe.getMembers());
        assertTrue(equipe.getMembers().isEmpty());
    }

    @Test
    void builder_customValues_workCorrectly() {
        EquipeRecherche equipe = EquipeRecherche.builder()
                .nom("AI Team")
                .description("Research")
                .members(List.of(new User()))
                .build();

        assertEquals(1, equipe.getMembers().size());
    }

    @Test
    void removeMember_userBelongsToEquipe_unlinksRelation() {
        EquipeRecherche equipe = new EquipeRecherche();
        User user = new User();

        user.setEquipeRecherche(equipe);
        equipe.getMembers().add(user);

        assertSame(equipe, user.getEquipeRecherche());

        equipe.removeMember(user);

        assertNull(user.getEquipeRecherche());
        assertFalse(equipe.getMembers().contains(user));
    }

    @Test
    void removeMember_userBelongsToAnotherEquipe_doesNotModifyRelation() {
        EquipeRecherche equipe1 = new EquipeRecherche();
        EquipeRecherche equipe2 = new EquipeRecherche();

        User user = new User();

        user.setEquipeRecherche(equipe2);
        equipe1.getMembers().add(user);

        equipe1.removeMember(user);

        assertSame(equipe2, user.getEquipeRecherche());
    }

    @Test
    void addMember_duplicateUser_addsTwice() {
        EquipeRecherche equipe = new EquipeRecherche();
        User user = new User();

        equipe.addMember(user);
        equipe.addMember(user);

        // List accepte les doublons ; la couche service empêche ce cas
        assertEquals(2, equipe.getMembers().size());
    }

    @Test
    void removeMember_nonExistentUser_doesNothing() {
        EquipeRecherche equipe = new EquipeRecherche();
        User user = new User();

        equipe.removeMember(user);

        assertTrue(equipe.getMembers().isEmpty());
    }

    // ---------------- statut ----------------

    @Test
    void statut_defaultIsActif() {
        EquipeRecherche equipe = new EquipeRecherche();
        assertEquals(StatutEquipe.ACTIF, equipe.getStatut());
    }

    @Test
    void statut_actif_setAndGet() {
        EquipeRecherche equipe = new EquipeRecherche();
        equipe.setStatut(StatutEquipe.INACTIF);
        assertEquals(StatutEquipe.INACTIF, equipe.getStatut());
    }

    @Test
    void statut_builderDefault_actif() {
        EquipeRecherche equipe = EquipeRecherche.builder()
                .nom("AI")
                .description("Desc")
                .build();
        assertEquals(StatutEquipe.ACTIF, equipe.getStatut());
    }

    @Test
    void statut_builderWithCustom_statutSet() {
        EquipeRecherche equipe = EquipeRecherche.builder()
                .nom("AI")
                .statut(StatutEquipe.INACTIF)
                .build();
        assertEquals(StatutEquipe.INACTIF, equipe.getStatut());
    }

    @Test
    void getMembers_returnsModifiableList() {
        EquipeRecherche equipe = new EquipeRecherche();
        User user = new User();
        equipe.addMember(user);

        List<User> members = equipe.getMembers();
        assertEquals(1, members.size());

        members.clear();
        assertTrue(equipe.getMembers().isEmpty()); // la liste est la même référence
    }
}
