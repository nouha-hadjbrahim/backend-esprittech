package tn.esprit.backend_esprittech.entity.industrialisation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.User;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "historique_industrialisation")
public class HistoriqueIndustrialisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private CandidatureIndustrialisation candidatureIndustrialisation;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatutIndustrialisation statutSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutIndustrialisation statutCible;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acteur_id", nullable = false)
    private User acteur;

    @Column(nullable = false)
    private String roleActeur;

    @Column(nullable = false)
    private LocalDateTime dateTransition;

    @Column(length = 2000)
    private String motif;

    @Column(length = 2000)
    private String commentaire;

    @PrePersist
    void onCreate() {
        if (dateTransition == null) {
            dateTransition = LocalDateTime.now();
        }
    }
}
