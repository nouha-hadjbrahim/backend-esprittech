package tn.esprit.backend_esprittech.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.notification.NotificationDTO;
import tn.esprit.backend_esprittech.entity.notification.Notification;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.repository.notification.NotificationRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int PAGE_SIZE = 20;

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository repository,
                               SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    public void creerNotification(Long userId, NotificationType type,
                                  String titre, String message, String lien) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(titre)
                .message(message)
                .link(lien)
                .build();
        Notification saved = repository.save(notification);

        NotificationDTO dto = toDTO(saved);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId), "/queue/notifications", dto);

        log.info("Notification creee et envoyee a l'utilisateur {} (type={}, destination=/user/{}/queue/notifications)",
                userId, type, userId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getParUtilisateur(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public long getNombreNonLues(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNonLuesParType(Long userId, NotificationType type) {
        return repository.findByUserIdAndTypeAndReadFalseOrderByCreatedAtDesc(userId, type)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void marquerCommeLu(Long id, Long userId) {
        repository.markAsRead(id, userId);
    }

    @Transactional
    public void marquerToutCommeLu(Long userId) {
        repository.markAllAsRead(userId);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        int deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Nettoyage notifications: {} notifications supprimees (plus de 90 jours)", deleted);
        }
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
