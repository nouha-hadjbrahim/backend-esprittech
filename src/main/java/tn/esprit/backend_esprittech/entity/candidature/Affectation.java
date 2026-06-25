package tn.esprit.backend_esprittech.entity.candidature;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.StatutAffectation;

import java.time.Instant;

/**
 * Représente l'assignation d'un étudiant à un sujet après acceptation de sa candidature.
 * Une affectation n'est JAMAIS supprimée (RG-09, RG-23) : elle passe à RETIREE_ARCHIVEE
 * si l'encadrant retire l'étudiant pendant la réalisation (US-22).
 */
@Entity
@Table(name = "affectations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sujet_id", nullable = false)
    private Long sujetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private User etudiant;

    /** Candidature ayant conduit à cette affectation (traçabilité) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false, unique = true)
    private Candidature candidature;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAffectation statut = StatutAffectation.ACTIVE;

    @Column(name = "date_debut", nullable = false, updatable = false)
    private Instant dateDebut;

    /** Renseigné uniquement si l'étudiant est retiré (US-22) */
    @Column(name = "date_retrait")
    private Instant dateRetrait;

    /** Motif obligatoire lors d'un retrait (US-22) */
    @Column(name = "motif_retrait", length = 1000)
    private String motifRetrait;

    @PrePersist
    void onCreate() {
        if (dateDebut == null) {
            dateDebut = Instant.now();
        }
    }
}
