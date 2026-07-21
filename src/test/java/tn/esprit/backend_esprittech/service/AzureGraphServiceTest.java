package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tn.esprit.backend_esprittech.config.AzureGraphProperties;
import tn.esprit.backend_esprittech.config.GraphTokenProvider;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.AzureGraphException;
import tn.esprit.backend_esprittech.exception.OrganizationMembershipException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AzureGraphServiceTest {

    private static final String GRAPH_BASE = "https://graph.example.tn";
    private static final String GROUPE_ENSEIGNANT = "grp-enseignant";
    private static final String GROUPE_ETUDIANT = "grp-etudiant";

    private MockRestServiceServer server;
    private AzureGraphService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(GRAPH_BASE);
        server = MockRestServiceServer.bindTo(builder).build();

        GraphTokenProvider tokenProvider = mock(GraphTokenProvider.class);
        when(tokenProvider.bearer()).thenReturn("Bearer jeton-test");

        service = new AzureGraphService(builder.build(), tokenProvider, properties());
    }

    @Test
    void resolveMember_memberOfStudentGroupOnly_shouldBeStudent() {
        expectUserLookup("user-1", "Genie Informatique");
        expectGroupCheck(GROUPE_ETUDIANT);

        var member = service.resolveMember("etudiant@esprit.tn");

        assertEquals(TypeUtilisateur.ETUDIANT, member.type());
        assertEquals("Genie Informatique", member.departement());
        server.verify();
    }

    @Test
    void resolveMember_memberOfTeacherGroup_shouldBeTeacher() {
        expectUserLookup("user-2", "Departement RDI");
        expectGroupCheck(GROUPE_ENSEIGNANT);

        assertEquals(TypeUtilisateur.ENSEIGNANT, service.resolveMember("prof@esprit.tn").type());
    }

    @Test
    void resolveMember_memberOfBothGroups_shouldPreferTeacher() {
        expectUserLookup("user-3", null);
        expectGroupCheck(GROUPE_ENSEIGNANT, GROUPE_ETUDIANT);

        var member = service.resolveMember("mixte@esprit.tn");

        assertEquals(TypeUtilisateur.ENSEIGNANT, member.type());
        assertNull(member.departement());
    }

    @Test
    void resolveMember_withoutAnyGroup_shouldDefaultToTeacher() {
        expectUserLookup("user-4", "Departement X");
        expectGroupCheck();

        assertEquals(TypeUtilisateur.ENSEIGNANT, service.resolveMember("inconnu@esprit.tn").type());
    }

    @Test
    void resolveMember_unknownEmail_shouldReportMembershipError() {
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));

        OrganizationMembershipException ex = assertThrows(OrganizationMembershipException.class,
                () -> service.resolveMember("etranger@gmail.com"));

        assertTrue(ex.getMessage().contains("n'appartient pas à l'organisation ESPRIT"));
    }

    @Test
    void resolveMember_lookupFailure_shouldReportGraphErrorWithEmail() {
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(withServerError());

        AzureGraphException ex = assertThrows(AzureGraphException.class,
                () -> service.resolveMember("prof@esprit.tn"));

        assertTrue(ex.getMessage().contains("prof@esprit.tn"));
    }

    @Test
    void resolveMember_groupCheckFailure_shouldReportGraphErrorWithUserId() {
        expectUserLookup("user-5", null);
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(withServerError());

        AzureGraphException ex = assertThrows(AzureGraphException.class,
                () -> service.resolveMember("prof@esprit.tn"));

        assertTrue(ex.getMessage().contains("user-5"));
    }

    @Test
    void resolveMember_shouldSendBearerTokenOnEveryCall() {
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andExpect(header("Authorization", "Bearer jeton-test"))
                .andRespond(withSuccess(userJson("user-6", "Dept"), MediaType.APPLICATION_JSON));
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andExpect(header("Authorization", "Bearer jeton-test"))
                .andRespond(withSuccess("{\"value\":[]}", MediaType.APPLICATION_JSON));

        service.resolveMember("prof@esprit.tn");

        server.verify();
    }

    private void expectUserLookup(String userId, String departement) {
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(withSuccess(userJson(userId, departement), MediaType.APPLICATION_JSON));
    }

    private void expectGroupCheck(String... groupes) {
        StringBuilder json = new StringBuilder("{\"value\":[");
        for (int i = 0; i < groupes.length; i++) {
            json.append(i > 0 ? "," : "").append('"').append(groupes[i]).append('"');
        }
        json.append("]}");
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
                .andRespond(withSuccess(json.toString(), MediaType.APPLICATION_JSON));
    }

    private String userJson(String id, String departement) {
        String dept = departement == null ? "null" : "\"" + departement + "\"";
        return "{\"value\":[{\"id\":\"" + id + "\",\"department\":" + dept + "}]}";
    }

    private AzureGraphProperties properties() {
        return new AzureGraphProperties(
                "tenant-esprit", "client-id", "client-secret",
                GROUPE_ENSEIGNANT, GROUPE_ETUDIANT,
                "https://login.example.tn", GRAPH_BASE, "noreply@esprit.tn");
    }
}
