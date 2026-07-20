package tn.esprit.backend_esprittech.service.critereeval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvaluationRequest;
import tn.esprit.backend_esprittech.dto.critereeval.MlEvaluationResponse;
import tn.esprit.backend_esprittech.exception.BusinessException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Service
public class MlEvaluationClient {

    private static final Logger log = LoggerFactory.getLogger(MlEvaluationClient.class);
    private static final Set<Integer> RETRYABLE_HTTP_STATUSES = Set.of(502, 503, 504);
    private static final Set<String> FATAL_EVALUATION_STATUSES =
            Set.of("FAILED_RETRYABLE", "FAILED_PERMANENT");
    private static final int MAX_ERROR_BODY_CHARS = 2_000;
    /** Cles JSON reutilisees dans les resumes de log ML. */
    private static final String CLE_CRITERIA = "criteria";
    private static final String CLE_SOURCE_TYPE = "sourceType";
    private static final String CLE_STATUS = "status";

    private final HttpClient httpClient;
    private final URI evaluationsUri;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration requestTimeout;
    private final int maxAttempts;
    private final long retryDelayMillis;
    private final int maxResponseChars;
    private final String requiredEngineVersion;

    public MlEvaluationClient(
            ObjectMapper objectMapper,
            @Value("${evaluation.ml.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${evaluation.ml.enabled:true}") boolean enabled,
            @Value("${evaluation.ml.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${evaluation.ml.request-timeout-seconds:300}") long requestTimeoutSeconds,
            @Value("${evaluation.ml.max-attempts:2}") int maxAttempts,
            @Value("${evaluation.ml.retry-delay-millis:750}") long retryDelayMillis,
            @Value("${evaluation.ml.max-response-chars:20000000}") int maxResponseChars,
            @Value("${evaluation.ml.required-engine-version:multimodal-semantic-v2}")
            String requiredEngineVersion
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.requestTimeout = Duration.ofSeconds(Math.max(1, requestTimeoutSeconds));
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelayMillis = Math.max(0, retryDelayMillis);
        this.maxResponseChars = Math.max(10_000, maxResponseChars);
        this.requiredEngineVersion = requiredEngineVersion == null
                ? "multimodal-semantic-v2"
                : requiredEngineVersion.trim();

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .build();

        this.evaluationsUri = URI.create(
                normalizeBaseUrl(baseUrl) + "/api/v1/evaluations"
        );
    }

    public MlEvaluationResponse evaluate(MlEvaluationRequest request) {
        validateRequest(request);

        String payload = toJson(request);
        logRequestSummary(payload);

        long started = System.nanoTime();
        String responsePayload = send(payload);
        long durationMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();

        validateResponseContract(responsePayload);
        logResponseSummary(responsePayload, durationMillis);

        MlEvaluationResponse response = fromJson(responsePayload);
        if (response == null || response.effectiveResults() == null) {
            throw new BusinessException("Le moteur ML a retourne une reponse vide.");
        }
        return response;
    }

    private void validateRequest(MlEvaluationRequest request) {
        if (!enabled) {
            throw new BusinessException("Le moteur ML d'evaluation est desactive.");
        }
        if (request == null || request.project() == null) {
            throw new BusinessException("La requete d'evaluation ML est invalide.");
        }
        if (request.criteria() == null || request.criteria().isEmpty()) {
            throw new BusinessException("La requete ML doit contenir au moins un critere.");
        }
    }

    private String toJson(MlEvaluationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            log.error("Impossible de serialiser la requete d'evaluation ML.", ex);
            throw new BusinessException(
                    "La requete d'evaluation ML ne peut pas etre serialisee."
            );
        }
    }

    private String send(String payload) {
        String correlationId = extractCorrelationId(payload);
        IOException lastIoException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(evaluationsUri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(requestTimeout)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Requested-Engine-Version", requiredEngineVersion)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            payload,
                            StandardCharsets.UTF_8
                    ));

            if (!correlationId.isBlank()) {
                builder.header("X-Correlation-Id", correlationId);
            }

            try {
                HttpResponse<String> response = httpClient.send(
                        builder.build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

                int statusCode = response.statusCode();
                String body = response.body() == null ? "" : response.body();

                if (statusCode >= 200 && statusCode < 300) {
                    if (body.length() > maxResponseChars) {
                        throw new BusinessException(
                                "La reponse ML depasse la taille autorisee."
                        );
                    }
                    return body;
                }

                if (RETRYABLE_HTTP_STATUSES.contains(statusCode)
                        && attempt < maxAttempts) {
                    log.warn(
                            "Evaluation ML temporairement indisponible: HTTP {}, tentative {}/{}.",
                            statusCode,
                            attempt,
                            maxAttempts
                    );
                    pauseBeforeRetry();
                    continue;
                }

                throw new BusinessException(
                        "Evaluation ML indisponible: "
                                + statusCode
                                + " "
                                + abbreviate(body, MAX_ERROR_BODY_CHARS)
                );

            } catch (IOException ex) {
                lastIoException = ex;
                if (attempt < maxAttempts) {
                    log.warn(
                            "Erreur reseau ML, tentative {}/{}: {}",
                            attempt,
                            maxAttempts,
                            ex.getMessage()
                    );
                    pauseBeforeRetry();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new BusinessException("Evaluation ML interrompue.");
            }
        }

        throw new BusinessException(
                "Evaluation ML indisponible: "
                        + (lastIoException == null
                        ? "erreur reseau inconnue"
                        : lastIoException.getMessage())
        );
    }

    private MlEvaluationResponse fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, MlEvaluationResponse.class);
        } catch (JsonProcessingException ex) {
            log.error(
                    "Impossible de lire la reponse ML. responseChars={}",
                    payload == null ? 0 : payload.length(),
                    ex
            );
            throw new BusinessException(
                    "La reponse d'evaluation ML ne peut pas etre lue."
            );
        }
    }

    private void validateResponseContract(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String evaluationStatus = root.path("evaluationStatus").asText("");
            String engineType = root.path("engineType").asText("");
            String pipelineVersion = root.path("pipelineVersion").asText("");
            JsonNode criteria = root.path(CLE_CRITERIA);
            JsonNode embedding = root.path("embedding");

            if (evaluationStatus.isBlank()) {
                throw new BusinessException(
                        "La reponse ML ne contient pas evaluationStatus."
                );
            }
            if (FATAL_EVALUATION_STATUSES.contains(evaluationStatus)) {
                throw new BusinessException(
                        "Le moteur ML a echoue: " + evaluationStatus
                );
            }
            if (!"ML_ASSISTED_MULTIMODAL".equals(engineType)) {
                throw new BusinessException("Moteur ML inattendu: " + engineType);
            }
            if (!requiredEngineVersion.equals(pipelineVersion)) {
                throw new BusinessException(
                        "Version ML inattendue: " + pipelineVersion
                );
            }
            if (!criteria.isArray() || criteria.isEmpty()) {
                throw new BusinessException(
                        "La reponse ML ne contient aucun resultat de critere."
                );
            }
            if (embedding.isObject()
                    && !embedding.path("available").asBoolean(false)) {
                throw new BusinessException(
                        "Le modele d'embeddings ML n'est pas disponible."
                );
            }
        } catch (JsonProcessingException ex) {
            throw new BusinessException("La reponse ML n'est pas un JSON valide.");
        }
    }

    private void logRequestSummary(String payload) {
        if (!log.isInfoEnabled()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode project = root.path("project");
            JsonNode criteria = root.path(CLE_CRITERIA);
            JsonNode deliverables = root.path("deliverables");
            JsonNode externalSources = root.path("externalSources");

            log.info(
                    "ML_REQUEST projectId={}, correlationId={}, engineVersion={}, "
                            + "criteriaVersion={}, criteria={}, deliverables={}, "
                            + "externalSources={}, payloadChars={}",
                    project.path("projectId").asLong(root.path("projectId").asLong()),
                    root.path("correlationId").asText(""),
                    root.path("engineVersion").asText(""),
                    root.path("criteriaVersion").asText(""),
                    criteria.isArray() ? criteria.size() : 0,
                    deliverables.isArray() ? deliverables.size() : 0,
                    externalSources.isArray() ? externalSources.size() : 0,
                    payload.length()
            );

            if (criteria.isArray()) {
                for (JsonNode criterion : criteria) {
                    log.info(
                            "ML_CRITERION id={}, type={}, family={}, "
                                    + "configurationVersion={}, policy={}, "
                                    + "strict={}, requiredEvidence={}, "
                                    + "optionalEvidence={}, positiveExpectations={}",
                            criterion.path("criterionId").asLong(),
                            criterion.path("type").asText(""),
                            criterion.path("criterionFamily").asText(""),
                            criterion.path("configurationVersion").asText(""),
                            criterion.path("scoringPolicy").asText(""),
                            criterion.path("strictConfiguration")
                                    .asBoolean(false),
                            criterion.path("requiredEvidenceTypes").size(),
                            criterion.path("optionalEvidenceTypes").size(),
                            criterion.path("positiveEvidenceExpectations").size()
                    );
                }
            }

            if (deliverables.isArray()) {
                for (JsonNode item : deliverables) {
                    log.info(
                            "ML_DELIVERABLE id={}, filename={}, sourceType={}, mimeType={}, "
                                    + "contentChars={}, base64Chars={}, hashPresent={}",
                            item.path("deliverableId").asLong(),
                            item.path("filename").asText(""),
                            item.path(CLE_SOURCE_TYPE).asText(""),
                            item.path("mimeType").asText(""),
                            item.path("content").asText("").length(),
                            item.path("contentBase64").asText("").length(),
                            !item.path("contentHash").asText("").isBlank()
                    );
                }
            }

            if (externalSources.isArray()) {
                for (JsonNode item : externalSources) {
                    log.info(
                            "ML_EXTERNAL_SOURCE type={}, host={}",
                            item.path(CLE_SOURCE_TYPE).asText(""),
                            safeHost(item.path("reference").asText(""))
                    );
                }
            }

        } catch (JsonProcessingException ex) {
            log.warn("Impossible de produire le resume de la requete ML.");
        }
    }

    private void logResponseSummary(String payload, long durationMillis) {
        if (!log.isInfoEnabled()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode criteria = root.path(CLE_CRITERIA);

            log.info(
                    "ML_RESPONSE status={}, engineType={}, pipelineVersion={}, "
                            + "globalConfidence={}, criteria={}, durationMs={}",
                    root.path("evaluationStatus").asText(""),
                    root.path("engineType").asText(""),
                    root.path("pipelineVersion").asText(""),
                    root.path("globalConfidence").asDouble(),
                    criteria.isArray() ? criteria.size() : 0,
                    durationMillis
            );

            if (criteria.isArray()) {
                for (JsonNode criterion : criteria) {
                    log.info(
                            "ML_RESULT id={}, type={}, status={}, "
                                    + "normalizedScore={}, confidence={}, "
                                    + "evidence={}, methods={}, reasons={}",
                            criterion.path("criterionId").asLong(),
                            criterion.path("type").asText(""),
                            criterion.path(CLE_STATUS).asText(""),
                            criterion.path("normalizedScore").isNumber()
                                    ? criterion.path("normalizedScore")
                                    .asDouble()
                                    : null,
                            criterion.path("confidence").asDouble(0.0),
                            criterion.path("evidence").size(),
                            criterion.path("analysisMethods").size(),
                            criterion.path("reasonCodes").toString()
                    );
                }
            }

            JsonNode processing = root.path("deliverableProcessing");
            if (processing.isArray()) {
                for (JsonNode item : processing) {
                    JsonNode metrics = item.path("metrics");
                    log.info(
                            "ML_PROCESSING filename={}, type={}, status={}, method={}, "
                                    + "pages={}, characters={}, chunks={}, warnings={}",
                            item.path("filename").asText(""),
                            item.path(CLE_SOURCE_TYPE).asText(""),
                            item.path(CLE_STATUS).asText(""),
                            item.path("extractionMethod").asText(""),
                            item.path("pageCount").asInt(0),
                            metrics.path("extractedCharacters").asLong(0),
                            metrics.path("chunkCount").asLong(0),
                            item.path("warnings").size()
                    );
                }
            }

            JsonNode externalProcessing = root.path("externalSourceProcessing");
            if (externalProcessing.isArray()) {
                for (JsonNode item : externalProcessing) {
                    JsonNode metrics = item.path("metrics");
                    log.info(
                            "ML_EXTERNAL_PROCESSING type={}, status={}, files={}, "
                                    + "sourceFiles={}, testFiles={}, remoteClone={}, commit={}",
                            item.path(CLE_SOURCE_TYPE).asText(""),
                            item.path(CLE_STATUS).asText(""),
                            metrics.path("file_count").asLong(0),
                            metrics.path("source_files").asLong(0),
                            metrics.path("test_files").asLong(0),
                            metrics.path("remote_clone").asBoolean(false),
                            abbreviate(metrics.path("commit_hash").asText(""), 12)
                    );
                }
            }

        } catch (JsonProcessingException ex) {
            log.warn("Impossible de produire le resume de la reponse ML.");
        }
    }

    private String extractCorrelationId(String payload) {
        try {
            return objectMapper.readTree(payload)
                    .path("correlationId")
                    .asText("");
        } catch (JsonProcessingException ex) {
            return "";
        }
    }

    private void pauseBeforeRetry() {
        if (retryDelayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    "Evaluation ML interrompue pendant une nouvelle tentative."
            );
        }
    }

    private String safeHost(String reference) {
        if (reference == null || reference.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(reference);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (URISyntaxException ex) {
            return "<invalid>";
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? "http://127.0.0.1:8000"
                : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String abbreviate(String value, int maximumLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maximumLength - 3)) + "...";
    }
}
