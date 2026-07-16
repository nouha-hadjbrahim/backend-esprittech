package tn.esprit.backend_esprittech.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.config.VerificationProperties;
import tn.esprit.backend_esprittech.entity.PendingRegistration;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailVerificationException;
import tn.esprit.backend_esprittech.repository.PendingRegistrationRepository;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Gère le cycle de vie de la vérification de propriété de l'email par code OTP :
 * création/mise à jour de l'inscription en attente, envoi du code par email,
 * validation et renvoi.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int CODE_BOUND = 1_000_000; // codes de 000000 à 999999

    private final PendingRegistrationRepository pendingRepository;
    private final GraphMailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationProperties properties;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(PendingRegistrationRepository pendingRepository,
                                    GraphMailService mailService,
                                    PasswordEncoder passwordEncoder,
                                    VerificationProperties properties) {
        this.pendingRepository = pendingRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    /**
     * Enregistre (ou remplace) une inscription en attente, génère un code OTP et
     * l'envoie par email. Toute inscription en attente précédente pour cet email
     * est écrasée.
     */
    @Transactional
    public void startVerification(PendingData data) {
        String code = generateCode();
        PendingRegistration pending = pendingRepository.findByEmail(data.email())
                .orElseGet(PendingRegistration::new);
        pending.setEmail(data.email());
        pending.setNom(data.nom());
        pending.setPrenom(data.prenom());
        pending.setPassword(data.encodedPassword());
        pending.setTypeUtilisateur(data.type());
        pending.setDepartement(data.departement());
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(Instant.now().plusSeconds(properties.codeTtlSeconds()));
        pending.setLastSentAt(Instant.now());
        pending.setAttempts(0);
        pendingRepository.save(pending);

        mailService.sendVerificationCode(data.email(), data.prenom(), code);
        log.info("Code de vérification envoyé pour l'inscription de {}", data.email());
    }

    /**
     * Valide le code fourni pour un email et, en cas de succès, supprime
     * l'inscription en attente et renvoie ses données pour créer le compte.
     *
     * @throws EmailVerificationException si aucun code n'est en attente, s'il est
     *         expiré, si le nombre de tentatives est dépassé ou si le code est faux.
     */
    @Transactional
    public PendingData consumeVerifiedRegistration(String email, String code) {
        PendingRegistration pending = pendingRepository.findByEmail(email)
                .orElseThrow(() -> new EmailVerificationException(
                        "Aucune demande d'inscription en attente pour cet email. Recommencez l'inscription."));

        if (Instant.now().isAfter(pending.getExpiresAt())) {
            pendingRepository.delete(pending);
            throw new EmailVerificationException("Le code a expiré. Recommencez l'inscription.");
        }
        if (pending.getAttempts() >= properties.maxAttempts()) {
            pendingRepository.delete(pending);
            throw new EmailVerificationException(
                    "Trop de tentatives. Recommencez l'inscription pour recevoir un nouveau code.");
        }
        if (!passwordEncoder.matches(code, pending.getCodeHash())) {
            pending.setAttempts(pending.getAttempts() + 1);
            pendingRepository.save(pending);
            throw new EmailVerificationException("Code invalide. Veuillez réessayer.");
        }

        PendingData data = PendingData.of(pending);
        pendingRepository.delete(pending);
        return data;
    }

    /** Régénère et renvoie un code pour une inscription en attente (avec cooldown anti-spam). */
    @Transactional
    public void resendCode(String email) {
        PendingRegistration pending = pendingRepository.findByEmail(email)
                .orElseThrow(() -> new EmailVerificationException(
                        "Aucune demande d'inscription en attente pour cet email. Recommencez l'inscription."));

        Instant nextAllowed = pending.getLastSentAt().plusSeconds(properties.resendCooldownSeconds());
        if (Instant.now().isBefore(nextAllowed)) {
            throw new EmailVerificationException(
                    "Un code a déjà été envoyé récemment. Veuillez patienter avant de redemander un code.");
        }

        String code = generateCode();
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(Instant.now().plusSeconds(properties.codeTtlSeconds()));
        pending.setLastSentAt(Instant.now());
        pending.setAttempts(0);
        pendingRepository.save(pending);

        mailService.sendVerificationCode(email, pending.getPrenom(), code);
        log.info("Code de vérification renvoyé pour {}", email);
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(CODE_BOUND));
    }

    /** Données d'une inscription en attente, transférées entre services. */
    public record PendingData(
            String email,
            String nom,
            String prenom,
            String encodedPassword,
            TypeUtilisateur type,
            String departement) {

        static PendingData of(PendingRegistration p) {
            return new PendingData(p.getEmail(), p.getNom(), p.getPrenom(),
                    p.getPassword(), p.getTypeUtilisateur(), p.getDepartement());
        }
    }
}
