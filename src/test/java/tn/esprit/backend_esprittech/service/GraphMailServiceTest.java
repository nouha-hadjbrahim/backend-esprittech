package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tn.esprit.backend_esprittech.config.AzureGraphProperties;
import tn.esprit.backend_esprittech.config.GraphTokenProvider;
import tn.esprit.backend_esprittech.config.VerificationProperties;
import tn.esprit.backend_esprittech.exception.AzureGraphException;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GraphMailServiceTest {

    private static final String GRAPH_BASE = "https://graph.example.tn";

    private MockRestServiceServer server;

    @Test
    void sendVerificationCode_shouldPostMailWithCodeAndBearerToken() {
        AtomicReference<String> body = new AtomicReference<>();
        GraphMailService service = newService("noreply@esprit.tn", 600);
        server.expect(anything())
                .andExpect(header("Authorization", "Bearer jeton-test"))
                .andExpect(request -> body.set(request.getBody().toString()))
                .andRespond(withSuccess());

        service.sendVerificationCode("etudiant@esprit.tn", "Ali", "123456");

        server.verify();
        assertTrue(body.get().contains("123456"), "le code doit figurer dans le message");
        assertTrue(body.get().contains("etudiant@esprit.tn"), "le destinataire doit etre renseigne");
        assertTrue(body.get().contains("Bonjour Ali,"), "le prenom doit personnaliser le message");
        assertTrue(body.get().contains("10 minute"), "la duree de validite doit etre calculee en minutes");
    }

    @Test
    void sendVerificationCode_withoutFirstName_shouldUseNeutralGreeting() {
        AtomicReference<String> body = new AtomicReference<>();
        GraphMailService service = newService("noreply@esprit.tn", 600);
        server.expect(anything())
                .andExpect(request -> body.set(request.getBody().toString()))
                .andRespond(withSuccess());

        service.sendVerificationCode("etudiant@esprit.tn", "  ", "123456");

        assertTrue(body.get().contains("Bonjour,"));
    }

    @Test
    void sendVerificationCode_shouldEscapeHtmlInFirstName() {
        AtomicReference<String> body = new AtomicReference<>();
        GraphMailService service = newService("noreply@esprit.tn", 600);
        server.expect(anything())
                .andExpect(request -> body.set(request.getBody().toString()))
                .andRespond(withSuccess());

        service.sendVerificationCode("etudiant@esprit.tn", "<script>", "123456");

        assertTrue(body.get().contains("&lt;script&gt;"), "le prenom doit etre echappe");
    }

    @Test
    void sendVerificationCode_veryShortTtl_shouldReportAtLeastOneMinute() {
        AtomicReference<String> body = new AtomicReference<>();
        GraphMailService service = newService("noreply@esprit.tn", 30);
        server.expect(anything())
                .andExpect(request -> body.set(request.getBody().toString()))
                .andRespond(withSuccess());

        service.sendVerificationCode("etudiant@esprit.tn", "Ali", "123456");

        assertTrue(body.get().contains("1 minute"));
    }

    @Test
    void sendMail_withoutConfiguredSender_shouldFailFast() {
        GraphMailService service = newService("  ", 600);

        AzureGraphException ex = assertThrows(AzureGraphException.class,
                () -> service.sendMail("dest@esprit.tn", "Sujet", "<p>corps</p>"));

        assertTrue(ex.getMessage().contains("Service d'email non configuré."));
        server.verify(); // aucune requete ne doit avoir ete emise
    }

    @Test
    void sendMail_graphFailure_shouldReportRecipientInError() {
        GraphMailService service = newService("noreply@esprit.tn", 600);
        server.expect(anything()).andRespond(withServerError());

        AzureGraphException ex = assertThrows(AzureGraphException.class,
                () -> service.sendMail("dest@esprit.tn", "Sujet", "<p>corps</p>"));

        assertTrue(ex.getMessage().contains("dest@esprit.tn"));
    }

    private GraphMailService newService(String sender, long codeTtlSeconds) {
        RestClient.Builder builder = RestClient.builder().baseUrl(GRAPH_BASE);
        server = MockRestServiceServer.bindTo(builder).build();

        GraphTokenProvider tokenProvider = mock(GraphTokenProvider.class);
        lenientBearer(tokenProvider);

        AzureGraphProperties graphProperties = new AzureGraphProperties(
                "tenant", "client", "secret", "grp-ens", "grp-etu",
                "https://login.example.tn", GRAPH_BASE, sender);

        return new GraphMailService(builder.build(), tokenProvider, graphProperties,
                new VerificationProperties(codeTtlSeconds, 5, 60));
    }

    private void lenientBearer(GraphTokenProvider tokenProvider) {
        when(tokenProvider.bearer()).thenReturn("Bearer jeton-test");
    }
}
