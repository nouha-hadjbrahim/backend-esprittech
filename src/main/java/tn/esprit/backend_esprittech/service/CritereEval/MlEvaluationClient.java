package tn.esprit.backend_esprittech.service.CritereEval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.MlEvaluationResponse;
import tn.esprit.backend_esprittech.exception.BusinessException;

@Service
public class MlEvaluationClient {

    private final RestClient restClient;
    private final boolean enabled;

    public MlEvaluationClient(
            RestClient.Builder builder,
            @Value("${evaluation.ml.base-url:http://localhost:8000}") String baseUrl,
            @Value("${evaluation.ml.enabled:true}") boolean enabled
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.enabled = enabled;
    }

    public MlEvaluationResponse evaluate(MlEvaluationRequest request) {
        if (!enabled) {
            throw new BusinessException("Le moteur ML d'evaluation est desactive.");
        }
        try {
            MlEvaluationResponse response = restClient.post()
                    .uri("/api/v1/evaluations")
                    .body(request)
                    .retrieve()
                    .body(MlEvaluationResponse.class);
            if (response == null || response.results() == null) {
                throw new BusinessException("Le moteur ML a retourne une reponse vide.");
            }
            return response;
        } catch (RestClientException ex) {
            throw new BusinessException("Evaluation ML indisponible: " + ex.getMessage());
        }
    }
}
