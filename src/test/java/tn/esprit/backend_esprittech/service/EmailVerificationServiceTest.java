package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.backend_esprittech.config.VerificationProperties;
import tn.esprit.backend_esprittech.entity.PendingRegistration;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailVerificationException;
import tn.esprit.backend_esprittech.repository.PendingRegistrationRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "etudiant@esprit.tn";

    @Mock private PendingRegistrationRepository pendingRepository;
    @Mock private GraphMailService mailService;
    @Mock private PasswordEncoder passwordEncoder;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(pendingRepository, mailService, passwordEncoder,
                new VerificationProperties(600, 3, 60));
    }

    // ---- startVerification ----

    @Test
    void startVerification_shouldPersistPendingRegistrationAndSendSixDigitCode() {
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash-code");

        service.startVerification(new EmailVerificationService.PendingData(
                EMAIL, "Mansour", "Ali", "hash-mdp", TypeUtilisateur.ETUDIANT, "GL"));

        ArgumentCaptor<PendingRegistration> captor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRepository).save(captor.capture());
        PendingRegistration saved = captor.getValue();
        assertEquals(EMAIL, saved.getEmail());
        assertEquals("hash-code", saved.getCodeHash());
        assertEquals(0, saved.getAttempts());

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendVerificationCode(eq(EMAIL), eq("Ali"), code.capture());
        assertTrue(code.getValue().matches("\\d{6}"), "le code doit comporter 6 chiffres");
    }

    @Test
    void startVerification_existingPendingRegistration_shouldBeReused() {
        PendingRegistration existing = pending(Instant.now().plusSeconds(600), 2);
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("nouveau-hash");

        service.startVerification(new EmailVerificationService.PendingData(
                EMAIL, "Mansour", "Ali", "hash-mdp", TypeUtilisateur.ETUDIANT, "GL"));

        verify(pendingRepository).save(existing);
        assertEquals(0, existing.getAttempts(), "les tentatives sont remises a zero");
    }

    // ---- consumeVerifiedRegistration ----

    @Test
    void consume_validCode_shouldReturnDataAndDeletePendingRegistration() {
        PendingRegistration pending = pending(Instant.now().plusSeconds(300), 0);
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("123456", "hash-code")).thenReturn(true);

        var data = service.consumeVerifiedRegistration(EMAIL, "123456");

        assertEquals(EMAIL, data.email());
        assertEquals(TypeUtilisateur.ETUDIANT, data.type());
        verify(pendingRepository).delete(pending);
    }

    @Test
    void consume_withoutPendingRegistration_shouldReject() {
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> service.consumeVerifiedRegistration(EMAIL, "123456"));

        assertTrue(ex.getMessage().contains("Aucune demande d'inscription en attente"));
    }

    @Test
    void consume_expiredCode_shouldDeleteRegistrationAndReject() {
        PendingRegistration pending = pending(Instant.now().minusSeconds(1), 0);
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.of(pending));

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> service.consumeVerifiedRegistration(EMAIL, "123456"));

        assertEquals("Le code a expiré. Recommencez l'inscription.", ex.getMessage());
        verify(pendingRepository).delete(pending);
    }

    @Test
    void consume_tooManyAttempts_shouldDeleteRegistrationAndReject() {
        PendingRegistration pending = pending(Instant.now().plusSeconds(300), 3);
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.of(pending));

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> service.consumeVerifiedRegistration(EMAIL, "123456"));

        assertTrue(ex.getMessage().contains("Trop de tentatives"));
        verify(pendingRepository).delete(pending);
    }

    @Test
    void consume_wrongCode_shouldIncrementAttempts() {
        PendingRegistration pending = pending(Instant.now().plusSeconds(300), 1);
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("000000", "hash-code")).thenReturn(false);

        assertThrows(EmailVerificationException.class,
                () -> service.consumeVerifiedRegistration(EMAIL, "000000"));

        assertEquals(2, pending.getAttempts());
        verify(pendingRepository).save(pending);
        verify(pendingRepository, never()).delete(any());
    }

    // ---- resendCode ----

    @Test
    void resendCode_afterCooldown_shouldSendNewCode() {
        PendingRegistration pending = pending(Instant.now().plusSeconds(300), 2);
        pending.setLastSentAt(Instant.now().minusSeconds(120));
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.of(pending));
        when(passwordEncoder.encode(anyString())).thenReturn("nouveau-hash");

        service.resendCode(EMAIL);

        assertEquals("nouveau-hash", pending.getCodeHash());
        assertEquals(0, pending.getAttempts());
        verify(pendingRepository).save(pending);
        verify(mailService).sendVerificationCode(eq(EMAIL), eq("Ali"), anyString());
    }

    @Test
    void resendCode_withinCooldown_shouldReject() {
        PendingRegistration pending = pending(Instant.now().plusSeconds(300), 0);
        pending.setLastSentAt(Instant.now());
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.of(pending));

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> service.resendCode(EMAIL));

        assertTrue(ex.getMessage().contains("Veuillez patienter"));
        verify(mailService, never()).sendVerificationCode(anyString(), anyString(), anyString());
    }

    @Test
    void resendCode_withoutPendingRegistration_shouldReject() {
        when(pendingRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(EmailVerificationException.class, () -> service.resendCode(EMAIL));
    }

    private PendingRegistration pending(Instant expiresAt, int attempts) {
        PendingRegistration pending = new PendingRegistration();
        pending.setEmail(EMAIL);
        pending.setNom("Mansour");
        pending.setPrenom("Ali");
        pending.setPassword("hash-mdp");
        pending.setTypeUtilisateur(TypeUtilisateur.ETUDIANT);
        pending.setDepartement("GL");
        pending.setCodeHash("hash-code");
        pending.setExpiresAt(expiresAt);
        pending.setLastSentAt(Instant.now());
        pending.setAttempts(attempts);
        return pending;
    }
}
