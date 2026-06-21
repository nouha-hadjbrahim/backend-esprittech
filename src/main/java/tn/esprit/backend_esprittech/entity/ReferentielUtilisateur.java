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
 * Référentiel de vérité importé par l'admin depuis un fichier CSV.
 * Une inscription n'est acceptée que si l'email OU l'identifiant fourni
 * correspond à une ligne de ce référentiel.
 */
@Entity
@Table(name = "referentiel_utilisateur", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ref_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_ref_identifiant", columnNames = "identifiant")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferentielUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String identifiant;

    /** ETUDIANT ou ENSEIGNANT : détermine le rôle assigné lors de l'inscription. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeUtilisateur type;

    private String telephone;

    private String departement;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @PrePersist
    void onCreate() {
        if (importedAt == null) {
            importedAt = Instant.now();
        }
    }
}
