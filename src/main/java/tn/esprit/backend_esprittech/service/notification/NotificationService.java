package tn.esprit.backend_esprittech.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.notification.NotificationDTO;
import tn.esprit.backend_esprittech.entity.notification.Notification;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.repository.notification.NotificationRepository;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

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
    public List<NotificationDTO> getParUtilisateur(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getNombreNonLues(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void marquerCommeLu(Long id, Long userId) {
        repository.markAsRead(id, userId);
    }

    @Transactional
    public void marquerToutCommeLu(Long userId) {
        repository.markAllAsRead(userId);
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
