package tn.esprit.backend_esprittech.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.backend_esprittech.config.AzureGraphProperties;
import tn.esprit.backend_esprittech.config.GraphTokenProvider;
import tn.esprit.backend_esprittech.config.VerificationProperties;
import tn.esprit.backend_esprittech.exception.AzureGraphException;

import java.util.List;
import java.util.Map;

/**
 * Envoi d'emails via Microsoft Graph (Outlook) avec la permission applicative
 * {@code Mail.Send}. L'expéditeur est la boîte ESPRIT configurée
 * ({@code azure.graph.mail-sender}).
 */
@Service
public class GraphMailService {

    private static final Logger log = LoggerFactory.getLogger(GraphMailService.class);

    private final RestClient graphClient;
    private final GraphTokenProvider tokenProvider;
    private final AzureGraphProperties graphProperties;
    private final VerificationProperties verificationProperties;

    public GraphMailService(@Qualifier("azureGraphRestClient") RestClient graphClient,
                            GraphTokenProvider tokenProvider,
                            AzureGraphProperties graphProperties,
                            VerificationProperties verificationProperties) {
        this.graphClient = graphClient;
        this.tokenProvider = tokenProvider;
        this.graphProperties = graphProperties;
        this.verificationProperties = verificationProperties;
    }

    /** Envoie le code de vérification (OTP) à l'adresse fournie. */
    public void sendVerificationCode(String toEmail, String prenom, String code) {
        long minutes = Math.max(1, verificationProperties.codeTtlSeconds() / 60);
        String subject = "EspriTECH — Code de vérification : " + code;
        String html = buildVerificationHtml(prenom, code, minutes);
        sendMail(toEmail, subject, html);
    }

    /** Envoie un email HTML via Graph {@code sendMail}. */
    public void sendMail(String toEmail, String subject, String htmlBody) {
        String sender = graphProperties.mailSender();
        if (!StringUtils.hasText(sender)) {
            log.error("Envoi d'email impossible : azure.graph.mail-sender (AZURE_MAIL_SENDER) non configuré.");
            throw new AzureGraphException("Service d'email non configuré.", null);
        }

        Map<String, Object> payload = Map.of(
                "message", Map.of(
                        "subject", subject,
                        "body", Map.of("contentType", "HTML", "content", htmlBody),
                        "toRecipients", List.of(
                                Map.of("emailAddress", Map.of("address", toEmail)))),
                "saveToSentItems", Boolean.FALSE);

        try {
            graphClient.post()
                    .uri("/users/{sender}/sendMail", sender)
                    .header(HttpHeaders.AUTHORIZATION, tokenProvider.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email envoyé à {} depuis {}", toEmail, sender);
        } catch (RestClientException e) {
            log.error("Échec de l'envoi d'email à {} via Graph", toEmail, e);
            throw new AzureGraphException("Envoi de l'email de vérification impossible pour le moment.", e);
        }
    }

    private String buildVerificationHtml(String prenom, String code, long minutes) {
        String salutation = StringUtils.hasText(prenom) ? "Bonjour " + escape(prenom) + "," : "Bonjour,";
        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;max-width:480px;margin:auto;color:#1a1a1a">
                  <h2 style="color:#E23E3E;margin-bottom:4px">EspriTECH</h2>
                  <p>%s</p>
                  <p>Voici votre code de vérification pour finaliser la création de votre compte :</p>
                  <p style="font-size:32px;font-weight:700;letter-spacing:8px;text-align:center;
                            background:#f5f5f5;border-radius:10px;padding:16px 0;margin:20px 0">%s</p>
                  <p>Ce code expire dans <strong>%d minute(s)</strong>. Si vous n'êtes pas à l'origine de
                     cette demande, ignorez simplement cet email.</p>
                  <p style="color:#888;font-size:12px;margin-top:24px">Email automatique — merci de ne pas répondre.</p>
                </div>
                """.formatted(salutation, escape(code), minutes);
    }

    /** Échappe le HTML minimal pour éviter toute injection dans le corps. */
    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
