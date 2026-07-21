package tn.esprit.backend_esprittech.service.critereeval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tn.esprit.backend_esprittech.dto.critereeval.MlCriterionRequest;
import tn.esprit.backend_esprittech.dto.critereeval.MlDeliverableContent;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvaluationRequest;
import tn.esprit.backend_esprittech.dto.critereeval.MlProjectContext;
import tn.esprit.backend_esprittech.exception.BusinessException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MlEvaluationClientTest {

    private static final String ENGINE_VERSION = "multimodal-semantic-v2";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void evaluate_shouldPostFastApiJsonPayload() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> acceptedContentType = new AtomicReference<>();
        AtomicReference<String> requestedEngineVersion = new AtomicReference<>();
        AtomicReference<String> receivedBody = new AtomicReference<>();

        HttpServer server = httpServer(
                method,
                contentType,
                acceptedContentType,
                requestedEngineVersion,
                receivedBody,
                200,
                validResponseJson()
        );
        server.start();

        try {
            int port = server.getAddress().getPort();
            MlEvaluationClient client = newClient(
                    "http://127.0.0.1:" + port,
                    true
            );

            var response = client.evaluate(validRequest());

            assertEquals("POST", method.get());
            assertThat(
                    contentType.get(),
                    containsString(MediaType.APPLICATION_JSON_VALUE)
            );
            assertThat(
                    acceptedContentType.get(),
                    containsString(MediaType.APPLICATION_JSON_VALUE)
            );
            assertEquals(
                    ENGINE_VERSION,
                    requestedEngineVersion.get()
            );

            assertNotNull(receivedBody.get());

            var sent = objectMapper.readTree(receivedBody.get());

            assertEquals(42L, sent.path("projectId").asLong());
            assertEquals(
                    ENGINE_VERSION,
                    sent.path("engineVersion").asText()
            );
            assertEquals(
                    20L,
                    sent.path("criteria").get(0)
                            .path("criterionId").asLong()
            );
            assertEquals(
                    100L,
                    sent.path("deliverables").get(0)
                            .path("deliverableId").asLong()
            );

            assertEquals("COMPLETED", response.status());
            assertNotNull(response.effectiveResults());
            assertEquals(1, response.effectiveResults().size());
            assertEquals(
                    20L,
                    response.effectiveResults().get(0).criterionId()
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluate_nullRequest_shouldFailBeforePostingEmptyBody() {
        MlEvaluationClient client = newClient(
                "http://127.0.0.1:8000",
                true
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.evaluate(null)
        );

        assertEquals(
                "La requete d'evaluation ML est invalide.",
                exception.getMessage()
        );
    }

    @Test
    void evaluate_whenDisabled_shouldFailBeforeHttpCall() {
        MlEvaluationClient client = newClient(
                "http://127.0.0.1:8000",
                false
        );

        var mlRequest = validRequest();
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.evaluate(mlRequest)
        );

        assertEquals(
                "Le moteur ML d'evaluation est desactive.",
                exception.getMessage()
        );
    }

    @Test
    void evaluate_whenFastApiReturnsUnexpectedEngine_shouldFail() throws Exception {
        HttpServer server = httpServer(
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                200,
                responseWithUnexpectedEngine()
        );
        server.start();

        try {
            int port = server.getAddress().getPort();
            MlEvaluationClient client = newClient(
                    "http://127.0.0.1:" + port,
                    true
            );

            var mlRequest = validRequest();
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> client.evaluate(mlRequest)
            );

            assertEquals(
                    "Moteur ML inattendu: LEGACY_KEYWORD",
                    exception.getMessage()
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluate_whenFastApiReturnsHttpError_shouldFail() throws Exception {
        HttpServer server = httpServer(
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                400,
                """
                {
                  "detail": "invalid evaluation request"
                }
                """
        );
        server.start();

        try {
            int port = server.getAddress().getPort();

            MlEvaluationClient client = new MlEvaluationClient(
                    objectMapper,
                    "http://127.0.0.1:" + port,
                    true,
                    10,
                    60,
                    1,
                    0,
                    2_000_000,
                    ENGINE_VERSION
            );

            var mlRequest = validRequest();
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> client.evaluate(mlRequest)
            );

            assertThat(
                    exception.getMessage(),
                    containsString("Evaluation ML indisponible: 400")
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluate_whenDisabled_shouldRejectWithoutCallingEngine() {
        MlEvaluationClient client = newClient("http://127.0.0.1:1", false);
        var request = validRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.evaluate(request));

        assertEquals("Le moteur ML d'evaluation est desactive.", exception.getMessage());
    }

    @Test
    void evaluate_withoutCriteria_shouldRejectBeforeAnyCall() {
        MlEvaluationClient client = newClient("http://127.0.0.1:1", true);
        var request = new MlEvaluationRequest(
                7L, "corr", "2026-07", ENGINE_VERSION, "hash", "projet", "livrables",
                new MlProjectContext(7L, "Titre", "Domaine", "Java", "RDI"),
                List.of(), List.of(), List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.evaluate(request));

        assertEquals("La requete ML doit contenir au moins un critere.", exception.getMessage());
    }

    @Test
    void evaluate_withNullProject_shouldReportInvalidRequest() {
        MlEvaluationClient client = newClient("http://127.0.0.1:1", true);
        var request = new MlEvaluationRequest(
                7L, "corr", "2026-07", ENGINE_VERSION, "hash", "projet", "livrables",
                null, List.of(), List.of(), List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.evaluate(request));

        assertEquals("La requete d'evaluation ML est invalide.", exception.getMessage());
    }

    @Test
    void evaluate_retryableStatus_shouldRetryThenSucceed() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/evaluations", exchange -> {
            boolean first = attempts.incrementAndGet() == 1;
            byte[] body = (first ? "{}" : validResponseJson()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(first ? 503 : 200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        try {
            MlEvaluationClient client = newClientWithAttempts(
                    "http://127.0.0.1:" + server.getAddress().getPort(), 2);

            assertNotNull(client.evaluate(validRequest()));
            assertEquals(2, attempts.get(), "le statut 503 doit declencher une seconde tentative");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluate_retryableStatusExhausted_shouldReportUnavailable() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/evaluations", exchange -> {
            attempts.incrementAndGet();
            byte[] body = "moteur indisponible".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(502, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        try {
            MlEvaluationClient client = newClientWithAttempts(
                    "http://127.0.0.1:" + server.getAddress().getPort(), 2);
            var request = validRequest();

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> client.evaluate(request));

            assertThat(exception.getMessage(), containsString("Evaluation ML indisponible: 502"));
            assertEquals(2, attempts.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluate_nonRetryableStatus_shouldFailOnFirstAttempt() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/evaluations", exchange -> {
            attempts.incrementAndGet();
            byte[] body = "requete invalide".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        try {
            MlEvaluationClient client = newClientWithAttempts(
                    "http://127.0.0.1:" + server.getAddress().getPort(), 3);
            var request = validRequest();

            assertThrows(BusinessException.class, () -> client.evaluate(request));
            assertEquals(1, attempts.get(), "un statut non reessayable ne doit pas etre retente");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void evaluate_networkFailure_shouldReportUnavailableAfterRetries() {
        // port ferme : provoque une IOException a chaque tentative
        MlEvaluationClient client = newClientWithAttempts("http://127.0.0.1:1", 2);
        var request = validRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.evaluate(request));

        assertThat(exception.getMessage(), containsString("Evaluation ML indisponible"));
    }

    @Test
    void evaluate_baseUrlWithTrailingSlashes_shouldStillReachEngine() throws Exception {
        AtomicReference<String> ignored = new AtomicReference<>();
        HttpServer server = httpServer(ignored, ignored, ignored, ignored, ignored, 200, validResponseJson());
        server.start();

        try {
            MlEvaluationClient client = newClient(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "///", true);

            assertNotNull(client.evaluate(validRequest()));
        } finally {
            server.stop(0);
        }
    }

    private MlEvaluationClient newClientWithAttempts(String baseUrl, int maxAttempts) {
        return new MlEvaluationClient(
                objectMapper, baseUrl, true, 10, 300, maxAttempts, 0, 20_000_000, ENGINE_VERSION);
    }

    private MlEvaluationClient newClient(
            String baseUrl,
            boolean enabled
    ) {
        return new MlEvaluationClient(
                objectMapper,
                baseUrl,
                enabled,
                10,
                300,
                1,
                0,
                20_000_000,
                ENGINE_VERSION
        );
    }

    private HttpServer httpServer(
            AtomicReference<String> method,
            AtomicReference<String> contentType,
            AtomicReference<String> acceptedContentType,
            AtomicReference<String> requestedEngineVersion,
            AtomicReference<String> receivedBody,
            int responseStatus,
            String responseJson
    ) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext(
                "/api/v1/evaluations",
                exchange -> {
                    method.set(exchange.getRequestMethod());
                    contentType.set(
                            exchange.getRequestHeaders()
                                    .getFirst(HttpHeaders.CONTENT_TYPE)
                    );
                    acceptedContentType.set(
                            exchange.getRequestHeaders()
                                    .getFirst(HttpHeaders.ACCEPT)
                    );
                    requestedEngineVersion.set(
                            exchange.getRequestHeaders()
                                    .getFirst(
                                            "X-Requested-Engine-Version"
                                    )
                    );
                    receivedBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    );

                    byte[] responseBody = responseJson.getBytes(
                            StandardCharsets.UTF_8
                    );

                    exchange.getResponseHeaders().set(
                            HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE
                    );
                    exchange.sendResponseHeaders(
                            responseStatus,
                            responseBody.length
                    );

                    try (var body = exchange.getResponseBody()) {
                        body.write(responseBody);
                    }
                }
        );

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
                List.of(
                        new MlCriterionRequest(
                                20L,
                                "NOTE",
                                "Qualite technique",
                                "Architecture, securite, tests et deploiement",
                                "Technique",
                                5.0,
                                2.0,
                                false
                        )
                ),
                List.of(
                        new MlDeliverableContent(
                                100L,
                                "RAPPORT",
                                "Rapport final",
                                "text/plain",
                                "READY",
                                null,
                                "Architecture API, securite JWT, "
                                        + "tests et deploiement Docker."
                        )
                )
        );
    }

    private String validResponseJson() {
        return """
                {
                  "evaluationStatus": "COMPLETED",
                  "status": "COMPLETED",
                  "engineType": "ML_ASSISTED_MULTIMODAL",
                  "modelName": "multilingual-e5-small",
                  "modelVersion": "multilingual-e5-small@main",
                  "modelRevision": "main",
                  "analysisEngineVersion": "multimodal-semantic-v2",
                  "pipelineVersion": "multimodal-semantic-v2",
                  "configurationVersion": "test-config",
                  "criteriaVersion": "test",
                  "inputSnapshotHash": "snapshot-hash",
                  "correlationId": "test-correlation",
                  "languages": ["fr"],
                  "embedding": {
                    "available": true,
                    "canEmbed": true,
                    "provider": "sentence-transformers",
                    "model": "multilingual-e5-small",
                    "version": "multilingual-e5-small@main"
                  },
                  "timing": {
                    "totalSeconds": 0.1
                  },
                  "globalConfidence": 0.8,
                  "strengths": [],
                  "weaknesses": [],
                  "recommendations": [],
                  "warnings": [],
                  "deliverableProcessing": [
                    {
                      "deliverableId": 100,
                      "filename": "Rapport final",
                      "sourceType": "TXT",
                      "status": "PROCESSED",
                      "language": "fr",
                      "contentHash": "hash",
                      "pageCount": null,
                      "extractionMethod": "TEXT_CONTENT",
                      "warnings": [],
                      "metrics": {
                        "extractedCharacters": 66,
                        "chunkCount": 1
                      }
                    }
                  ],
                  "externalSourceProcessing": [],
                  "criteria": [
                    {
                      "criterionId": 20,
                      "type": "NOTE",
                      "status": "SCORED",
                      "score": 4.0,
                      "maxScore": 5.0,
                      "normalizedScore": 0.8,
                      "confidence": 0.7,
                      "semanticConfidence": 0.7,
                      "overallConfidence": 0.7,
                      "evidenceQuality": 0.8,
                      "extractionQuality": 1.0,
                      "sourceCoverage": 1.0,
                      "modelConfidence": 0.7,
                      "satisfied": null,
                      "eliminatoryState": null,
                      "analysisMethods": [
                        "PRETRAINED_EMBEDDING_MODEL"
                      ],
                      "justification": "Contenu analyse",
                      "reasonCodes": [],
                      "scoreBand": "strong evidence",
                      "evidence": [],
                      "strengths": [],
                      "weaknesses": [],
                      "recommendations": []
                    }
                  ],
                  "results": [
                    {
                      "criterionId": 20,
                      "type": "NOTE",
                      "status": "SCORED",
                      "score": 4.0,
                      "maxScore": 5.0,
                      "normalizedScore": 0.8,
                      "confidence": 0.7,
                      "semanticConfidence": 0.7,
                      "overallConfidence": 0.7,
                      "evidenceQuality": 0.8,
                      "extractionQuality": 1.0,
                      "sourceCoverage": 1.0,
                      "modelConfidence": 0.7,
                      "satisfied": null,
                      "eliminatoryState": null,
                      "analysisMethods": [
                        "PRETRAINED_EMBEDDING_MODEL"
                      ],
                      "justification": "Contenu analyse",
                      "reasonCodes": [],
                      "scoreBand": "strong evidence",
                      "evidence": [],
                      "strengths": [],
                      "weaknesses": [],
                      "recommendations": []
                    }
                  ]
                }
                """;
    }

    private String responseWithUnexpectedEngine() {
        return """
                {
                  "evaluationStatus": "COMPLETED",
                  "status": "COMPLETED",
                  "engineType": "LEGACY_KEYWORD",
                  "pipelineVersion": "multimodal-semantic-v2",
                  "embedding": {
                    "available": true
                  },
                  "criteria": [
                    {
                      "criterionId": 20,
                      "type": "NOTE",
                      "status": "SCORED",
                      "score": 4.0,
                      "maxScore": 5.0,
                      "normalizedScore": 0.8,
                      "confidence": 0.7,
                      "evidence": []
                    }
                  ],
                  "results": [
                    {
                      "criterionId": 20,
                      "type": "NOTE",
                      "status": "SCORED",
                      "score": 4.0,
                      "maxScore": 5.0,
                      "normalizedScore": 0.8,
                      "confidence": 0.7,
                      "evidence": []
                    }
                  ]
                }
                """;
    }
}
