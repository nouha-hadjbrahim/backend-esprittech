package tn.esprit.backend_esprittech.entity.candidature;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.StatutCandidature;

import java.time.Instant;

@Entity
@Table(name = "candidatures", uniqueConstraints = {
        @UniqueConstraint(name = "uk_candidature_sujet_etudiant",
                columnNames = {"sujet_id", "etudiant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK vers SujetProjet (stockée comme Long, cohérent avec encadrantId dans SujetProjet) */
    @Column(name = "sujet_id", nullable = false)
    private Long sujetId;

    /** FK vers User (étudiant) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private User etudiant;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCandidature statut = StatutCandidature.DEPOSEE;

    /** Motif obligatoire si statut = REFUSEE (RG-08) */
    @Column(name = "motif_refus", length = 1000)
    private String motifRefus;

    /** Message libre de l'étudiant lors de la candidature */
    @Column(name = "message_etudiant", length = 2000)
    private String messageEtudiant;

    /** Date à laquelle l'encadrant a pris sa décision (acceptation ou refus) */
    @Column(name = "date_decision")
    private Instant dateDecision;

    @Column(name = "date_depot", nullable = false, updatable = false)
    private Instant dateDepot;

    @PrePersist
    void onDepose() {
        if (dateDepot == null) {
            dateDepot = Instant.now();
        }
    }
}
