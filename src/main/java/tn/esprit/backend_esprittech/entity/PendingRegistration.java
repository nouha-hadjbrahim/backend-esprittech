package tn.esprit.backend_esprittech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

import java.time.Instant;

/**
 * Inscription en attente de vérification : conserve les données saisies et le
 * code OTP (haché) jusqu'à ce que l'utilisateur prouve qu'il possède l'email.
 * Le compte {@link User} n'est créé qu'une fois le code validé.
 */
@Entity
@Table(name = "pending_registration", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pending_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    /** Mot de passe déjà encodé (BCrypt). */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_utilisateur", nullable = false, length = 20)
    private TypeUtilisateur typeUtilisateur;

    private String departement;

    /** Code OTP haché (BCrypt) — jamais stocké en clair. */
    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
