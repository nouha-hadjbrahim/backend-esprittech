package tn.esprit.backend_esprittech.service.CritereEval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationResponse;
import tn.esprit.backend_esprittech.exception.BusinessException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class MlEvaluationClient {

    private static final Logger log = LoggerFactory.getLogger(MlEvaluationClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final URI evaluationsUri;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public MlEvaluationClient(
            ObjectMapper objectMapper,
            @Value("${evaluation.ml.base-url:http://localhost:8000}") String baseUrl,
            @Value("${evaluation.ml.enabled:true}") boolean enabled
    ) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.evaluationsUri = URI.create(normalizeBaseUrl(baseUrl) + "/api/v1/evaluations");
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public MlEvaluationResponse evaluate(MlEvaluationRequest request) {
        if (!enabled) {
            throw new BusinessException("Le moteur ML d'evaluation est desactive.");
        }
        if (request == null || request.project() == null) {
            throw new BusinessException("La requete d'evaluation ML est invalide.");
        }
        String payload = toJson(request);
        log.info("Envoi evaluation ML projet={}, criteres={}, livrables={}, payloadChars={}, firstChar={}",
                request.project().projectId(),
                request.criteria() == null ? 0 : request.criteria().size(),
                request.deliverables() == null ? 0 : request.deliverables().size(),
                payload.length(),
                payload.isBlank() ? "<blank>" : payload.substring(0, 1));
        MlEvaluationResponse response = fromJson(send(payload));
        if (response == null || response.results() == null) {
            throw new BusinessException("Le moteur ML a retourne une reponse vide.");
        }
        return response;
    }

    private String toJson(MlEvaluationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("La requete d'evaluation ML ne peut pas etre serialisee.");
        }
    }

    private String send(String payload) {
        HttpRequest httpRequest = HttpRequest.newBuilder(evaluationsUri)
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("Evaluation ML indisponible: "
                        + response.statusCode() + " " + response.body());
            }
            return response.body();
        } catch (IOException ex) {
            throw new BusinessException("Evaluation ML indisponible: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Evaluation ML interrompue.");
        }
    }

    private MlEvaluationResponse fromJson(String payload) {
        try {
            return objectMapper.readValue(payload, MlEvaluationResponse.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("La reponse d'evaluation ML ne peut pas etre lue.");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank()
                ? "http://localhost:8000"
                : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
