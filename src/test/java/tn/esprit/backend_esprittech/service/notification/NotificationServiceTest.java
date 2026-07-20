package tn.esprit.backend_esprittech.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tn.esprit.backend_esprittech.dto.notification.NotificationDTO;
import tn.esprit.backend_esprittech.entity.notification.Notification;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.repository.notification.NotificationRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificationService service;

    private Notification notification;
    private NotificationDTO dto;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.CANDIDATURE_RECUE)
                .title("Candidature recue")
                .message("Un etudiant a depose une candidature.")
                .link("/frontoffice/mes-projets")
                .read(false)
                .createdAt(Instant.parse("2026-07-15T10:00:00Z"))
                .build();
        dto = new NotificationDTO(
                1L, NotificationType.CANDIDATURE_RECUE,
                "Candidature recue", "Un etudiant a depose une candidature.",
                "/frontoffice/mes-projets", false,
                Instant.parse("2026-07-15T10:00:00Z"));
    }

    @Test
    void creerNotification_savesAndPushesToWebSocket() {
        when(repository.save(any(Notification.class))).thenReturn(notification);

        service.creerNotification(10L, NotificationType.CANDIDATURE_RECUE,
                "Candidature recue", "Un etudiant a depose une candidature.",
                "/frontoffice/mes-projets");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(10L, saved.getUserId());
        assertEquals(NotificationType.CANDIDATURE_RECUE, saved.getType());

        verify(messagingTemplate).convertAndSendToUser(
                eq("10"), eq("/queue/notifications"), any(NotificationDTO.class));
    }

    @Test
    void creerNotification_allFieldsMappedToDTO() {
        when(repository.save(any(Notification.class))).thenReturn(notification);

        service.creerNotification(10L, NotificationType.CANDIDATURE_RECUE,
                "Candidature recue", "Un etudiant a depose une candidature.",
                "/frontoffice/mes-projets");

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("10"), eq("/queue/notifications"), dtoCaptor.capture());
        NotificationDTO sent = dtoCaptor.getValue();
        assertEquals(1L, sent.id());
        assertEquals(NotificationType.CANDIDATURE_RECUE, sent.type());
        assertEquals("Candidature recue", sent.title());
        assertEquals("Un etudiant a depose une candidature.", sent.message());
        assertEquals("/frontoffice/mes-projets", sent.link());
        assertFalse(sent.read());
        assertEquals(Instant.parse("2026-07-15T10:00:00Z"), sent.createdAt());
    }

    @Test
    void getParUtilisateur_returnsPaginatedNotifications() {
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1);
        when(repository.findByUserIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 20))).thenReturn(page);

        Page<NotificationDTO> result = service.getParUtilisateur(10L, 0);

        assertEquals(1, result.getContent().size());
        assertEquals(NotificationType.CANDIDATURE_RECUE, result.getContent().get(0).type());
        assertEquals(0, result.getNumber());
        assertTrue(result.isLast());
    }

    @Test
    void getParUtilisateur_emptyPage() {
        Page<Notification> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(repository.findByUserIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 20))).thenReturn(emptyPage);

        Page<NotificationDTO> result = service.getParUtilisateur(10L, 0);

        assertTrue(result.getContent().isEmpty());
        assertTrue(result.isLast());
    }

    @Test
    void getNombreNonLues_delegatesToRepository() {
        when(repository.countByUserIdAndReadFalse(10L)).thenReturn(5L);

        long count = service.getNombreNonLues(10L);

        assertEquals(5L, count);
    }

    @Test
    void getNonLuesParType_returnsUnreadSujetValide() {
        Notification valide = Notification.builder()
                .id(2L)
                .userId(10L)
                .type(NotificationType.SUJET_VALIDE)
                .title("Sujet accepté")
                .message("Votre sujet \"IA\" a été accepté.")
                .link("/frontoffice/sujets/mes-sujets")
                .read(false)
                .createdAt(Instant.parse("2026-07-15T11:00:00Z"))
                .build();
        when(repository.findByUserIdAndTypeAndReadFalseOrderByCreatedAtDesc(10L, NotificationType.SUJET_VALIDE))
                .thenReturn(List.of(valide));

        List<NotificationDTO> result = service.getNonLuesParType(10L, NotificationType.SUJET_VALIDE);

        assertEquals(1, result.size());
        assertEquals(NotificationType.SUJET_VALIDE, result.get(0).type());
        assertEquals(2L, result.get(0).id());
    }

    @Test
    void marquerCommeLu_delegatesToRepository() {
        when(repository.markAsRead(1L, 10L)).thenReturn(1);

        service.marquerCommeLu(1L, 10L);

        verify(repository).markAsRead(1L, 10L);
    }

    @Test
    void marquerToutCommeLu_delegatesToRepository() {
        when(repository.markAllAsRead(10L)).thenReturn(3);

        service.marquerToutCommeLu(10L);

        verify(repository).markAllAsRead(10L);
    }

    @Test
    void cleanupOldNotifications_deletesOldEntries() {
        when(repository.deleteOlderThan(any(Instant.class))).thenReturn(42);

        service.cleanupOldNotifications();

        verify(repository).deleteOlderThan(any(Instant.class));
    }

    @Test
    void cleanupOldNotifications_noDeletionWhenNoneOld() {
        when(repository.deleteOlderThan(any(Instant.class))).thenReturn(0);

        service.cleanupOldNotifications();

        verify(repository).deleteOlderThan(any(Instant.class));
    }

    @Test
    void creerNotification_nullLink_succeeds() {
        notification.setLink(null);
        when(repository.save(any(Notification.class))).thenReturn(notification);

        service.creerNotification(10L, NotificationType.SUJET_DEMANDE,
                "Sujet demande", "Un sujet a ete soumis.", null);

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("10"), eq("/queue/notifications"), dtoCaptor.capture());
        assertNull(dtoCaptor.getValue().link());
    }

    @Test
    void getParUtilisateur_page2() {
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(1, 20), 45);
        when(repository.findByUserIdOrderByCreatedAtDesc(10L, PageRequest.of(1, 20))).thenReturn(page);

        Page<NotificationDTO> result = service.getParUtilisateur(10L, 1);

        assertEquals(1, result.getNumber());
        assertFalse(result.isFirst());
    }

    @Test
    void creerNotification_defaultsReadToFalse() {
        when(repository.save(any(Notification.class))).thenReturn(notification);

        service.creerNotification(10L, NotificationType.CANDIDATURE_RECUE,
                "Candidature recue", "Un etudiant a depose une candidature.",
                "/frontoffice/mes-projets");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertFalse(captor.getValue().isRead());
    }

    @Test
    void toDTO_mapsReadTrueCorrectly() {
        notification.setRead(true);
        when(repository.save(any(Notification.class))).thenReturn(notification);

        service.creerNotification(10L, NotificationType.AFFILIATION_ACCEPTEE,
                "Affiliation acceptee", "Votre affiliation a ete acceptee.",
                "/frontoffice/projets");

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("10"), eq("/queue/notifications"), dtoCaptor.capture());
        assertTrue(dtoCaptor.getValue().read());
    }

    @Test
    void toDTO_mapsDifferentNotificationTypes() {
        notification.setType(NotificationType.EVALUATION_CALCULEE);
        when(repository.save(any(Notification.class))).thenReturn(notification);

        service.creerNotification(10L, NotificationType.EVALUATION_CALCULEE,
                "Evaluation calculee", "Une evaluation a ete calculee.",
                "/frontoffice/evaluations");

        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("10"), eq("/queue/notifications"), dtoCaptor.capture());
        assertEquals(NotificationType.EVALUATION_CALCULEE, dtoCaptor.getValue().type());
    }
}
