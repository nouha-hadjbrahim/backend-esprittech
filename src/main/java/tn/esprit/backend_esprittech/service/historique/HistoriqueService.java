package tn.esprit.backend_esprittech.service.historique;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.response.HistoriqueResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.historique.Historique;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.repository.historique.HistoriqueRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class HistoriqueService {

    private final HistoriqueRepository repository;
    private final ObjectMapper objectMapper;

    public HistoriqueService(HistoriqueRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void log(ActionType action, String entityType, Long entityId, String summary,
                    Map<String, Object> oldValues, Map<String, Object> newValues,
                    Map<String, Object> metadata, User actor, String actorRole) {
        Historique historique = Historique.builder()
                .actor(actor)
                .actorRole(actorRole)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(summary)
                .oldValues(toJson(oldValues))
                .newValues(toJson(newValues))
                .metadata(toJson(metadata))
                .build();
        repository.save(historique);
    }

    public PagedResponse<HistoriqueResponse> search(String entityType, Long entityId,
                                                      ActionType action, Long actorId,
                                                      LocalDateTime dateFrom, LocalDateTime dateTo,
                                                      Pageable pageable) {
        Page<Historique> page = repository.search(entityType, entityId, action, actorId, dateFrom, dateTo, pageable);
        return PagedResponse.from(page, HistoriqueResponse::from);
    }

    public PagedResponse<HistoriqueResponse> findByEntity(String entityType, Long entityId, Pageable pageable) {
        Page<Historique> page = repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
        return PagedResponse.from(page, HistoriqueResponse::from);
    }

    public PagedResponse<HistoriqueResponse> findByUser(Long userId, Pageable pageable) {
        Page<Historique> page = repository.findByActorIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page, HistoriqueResponse::from);
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
