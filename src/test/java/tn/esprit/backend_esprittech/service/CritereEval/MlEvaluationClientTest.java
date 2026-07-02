package tn.esprit.backend_esprittech.service.CritereEval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tn.esprit.backend_esprittech.dto.CritereEval.MlCriterionRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlDeliverableContent;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlProjectContext;
import tn.esprit.backend_esprittech.exception.BusinessException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MlEvaluationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void evaluate_shouldPostFastApiJsonPayload() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();
        HttpServer server = httpServer(method, contentType, receivedBody);
        server.start();

        try {
            int port = server.getAddress().getPort();
            MlEvaluationClient client = new MlEvaluationClient(objectMapper, "http://localhost:" + port, true);

            var response = client.evaluate(validRequest());

            assertEquals("POST", method.get());
            assertThat(contentType.get(), containsString(MediaType.APPLICATION_JSON_VALUE));
            assertNotNull(receivedBody.get());
            assertEquals(objectMapper.readTree(expectedRequestJson()), objectMapper.readTree(receivedBody.get()));
            assertEquals("COMPLETED", response.status());
            assertEquals(1, response.results().size());
            assertEquals(20L, response.results().get(0).criterionId());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluate_nullRequest_shouldFailBeforePostingEmptyBody() {
        MlEvaluationClient client = new MlEvaluationClient(objectMapper, "http://ml.local", true);

        BusinessException exception = assertThrows(BusinessException.class, () -> client.evaluate(null));

        assertEquals("La requete d'evaluation ML est invalide.", exception.getMessage());
    }

    private HttpServer httpServer(AtomicReference<String> method,
                                  AtomicReference<String> contentType,
                                  AtomicReference<String> receivedBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/evaluations", exchange -> {
            method.set(exchange.getRequestMethod());
            contentType.set(exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] responseBody = validResponseJson().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, responseBody.length);
            try (var body = exchange.getResponseBody()) {
                body.write(responseBody);
            }
        });
        return server;
    }

    private MlEvaluationRequest validRequest() {
        return new MlEvaluationRequest(
                new MlProjectContext(
                        42L,
                        "Plateforme IoT",
                        "Industrialisation",
                        "Spring Boot, Angular, Docker",
                        "IOT"
                ),
                List.of(new MlCriterionRequest(
                        20L,
                        "NOTE",
                        "Qualite technique",
                        "Architecture, securite, tests et deploiement",
                        "Technique",
                        5.0,
                        2.0,
                        false
                )),
                List.of(new MlDeliverableContent(
                        100L,
                        "RAPPORT",
                        "Rapport final",
                        "text/plain",
                        "READY",
                        null,
                        "Architecture API, securite JWT, tests et deploiement Docker."
                ))
        );
    }

    private String expectedRequestJson() {
        return """
                {
                  "project": {
                    "projectId": 42,
                    "title": "Plateforme IoT",
                    "domain": "Industrialisation",
                    "technologies": "Spring Boot, Angular, Docker",
                    "category": "IOT"
                  },
                  "criteria": [
                    {
                      "criterionId": 20,
                      "type": "NOTE",
                      "label": "Qualite technique",
                      "description": "Architecture, securite, tests et deploiement",
                      "domain": "Technique",
                      "maxScore": 5.0,
                      "weight": 2.0,
                      "eliminatory": false
                    }
                  ],
                  "deliverables": [
                    {
                      "deliverableId": 100,
                      "deliverableType": "RAPPORT",
                      "name": "Rapport final",
                      "contentType": "text/plain",
                      "extractionStatus": "READY",
                      "unsupportedReason": null,
                      "content": "Architecture API, securite JWT, tests et deploiement Docker."
                    }
                  ]
                }
                """;
    }

    private String validResponseJson() {
        return """
                {
                  "status": "COMPLETED",
                  "modelVersion": "deliverable-content-evaluator-v1",
                  "analysisEngineVersion": "deliverable-content-evaluator-v1",
                  "globalConfidence": 0.8,
                  "strengths": [],
                  "weaknesses": [],
                  "recommendations": [],
                  "warnings": [],
                  "results": [
                    {
                      "criterionId": 20,
                      "type": "NOTE",
                      "score": 4.0,
                      "maxScore": 5.0,
                      "normalizedScore": 0.8,
                      "confidence": 0.7,
                      "satisfied": null,
                      "justification": "Contenu analyse",
                      "evidence": [],
                      "strengths": [],
                      "weaknesses": [],
                      "recommendations": []
                    }
                  ]
                }
                """;
    }
}
