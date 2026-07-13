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
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.historique.HistoriqueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.exception.ForbiddenException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HistoriqueService {

    public record LogRequest(
            ActionType action,
            EntityType entityType,
            Long entityId,
            String summary,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            Map<String, Object> metadata,
            User actor,
            Role actorRole
    ) {}

    private final HistoriqueRepository repository;
    private final ObjectMapper objectMapper;
    private final CandidatureRepository candidatureRepository;
    private final SujetProjetRepository sujetProjetRepository;
    private final UserService userService;

    public HistoriqueService(HistoriqueRepository repository,
                             ObjectMapper objectMapper,
                             CandidatureRepository candidatureRepository,
                             SujetProjetRepository sujetProjetRepository,
                             UserService userService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.candidatureRepository = candidatureRepository;
        this.sujetProjetRepository = sujetProjetRepository;
        this.userService = userService;
    }

    @Transactional
    public void log(LogRequest request) {
        Historique historique = Historique.builder()
                .actor(request.actor())
                .actorRole(request.actorRole())
                .action(request.action())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .summary(request.summary())
                .oldValues(toJson(request.oldValues()))
                .newValues(toJson(request.newValues()))
                .metadata(toJson(request.metadata()))
                .build();
        repository.save(historique);
    }

    public PagedResponse<HistoriqueResponse> search(EntityType entityType, Long entityId,
                                                      ActionType action, Long actorId,
                                                      LocalDateTime dateFrom, LocalDateTime dateTo,
                                                      Pageable pageable) {
        Page<Historique> page = repository.search(entityType, entityId, action, actorId, dateFrom, dateTo, pageable);
        return PagedResponse.from(page, HistoriqueResponse::from);
    }

    public PagedResponse<HistoriqueResponse> findByEntity(EntityType entityType, Long entityId, Pageable pageable) {
        Page<Historique> page = repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
        return PagedResponse.from(page, HistoriqueResponse::from);
    }

    public PagedResponse<HistoriqueResponse> findByUser(Long userId, Pageable pageable) {
        Page<Historique> page = repository.findByActorIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page, HistoriqueResponse::from);
    }

    /**
     * Historique agrégé d'un sujet.
     * Filtre :
     * - TOUT : tous les événements
     * - SUJET : tout sauf acceptation/refus candidature
     * - CANDIDATURE : uniquement ACCEPT / REFUSE
     */
    @Transactional(readOnly = true)
    public List<HistoriqueResponse> findBySujet(Long sujetId, String filter) {
        ensureCanViewSujetHistorique(sujetId);

        String normalized = filter == null || filter.isBlank() ? "TOUT" : filter.trim().toUpperCase();
        List<Historique> events = new ArrayList<>();

        if ("TOUT".equals(normalized) || "SUJET".equals(normalized)) {
            events.addAll(repository.findAllBySujetProjetId(sujetId));
        }

        Set<Long> candidatureIds = candidatureRepository.findBySujetIdOrderByDateDepotAsc(sujetId)
                .stream()
                .map(c -> c.getId())
                .collect(Collectors.toSet());

        if (!candidatureIds.isEmpty()) {
            List<Historique> candidatureEvents = repository.findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(
                    EntityType.CANDIDATURE, candidatureIds);

            if ("CANDIDATURE".equals(normalized)) {
                candidatureEvents = candidatureEvents.stream()
                        .filter(h -> h.getAction() == ActionType.ACCEPT || h.getAction() == ActionType.REFUSE)
                        .toList();
            } else if ("SUJET".equals(normalized)) {
                candidatureEvents = candidatureEvents.stream()
                        .filter(h -> h.getAction() != ActionType.ACCEPT && h.getAction() != ActionType.REFUSE)
                        .toList();
            }

            if ("TOUT".equals(normalized) || "SUJET".equals(normalized) || "CANDIDATURE".equals(normalized)) {
                events.addAll(candidatureEvents);
            }
        }

        return events.stream()
                .sorted(Comparator.comparing(Historique::getCreatedAt).reversed())
                .map(HistoriqueResponse::from)
                .collect(Collectors.toList());
    }

    private void ensureCanViewSujetHistorique(Long sujetId) {
        SujetProjet sujet = sujetProjetRepository.findById(sujetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sujet introuvable (id=" + sujetId + ")"));
        User current = userService.getCurrentUser();
        Role role = current.getRole();

        if (role == Role.ROLE_ADMIN || role == Role.ROLE_CI || role == Role.ROLE_CHEF_EQUIPE) {
            return;
        }
        if (role == Role.ROLE_ENSEIGNANT && sujet.getEncadrantId().equals(current.getId())) {
            return;
        }
        throw new ForbiddenException("Vous n'avez pas accès à l'historique de ce sujet.");
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
