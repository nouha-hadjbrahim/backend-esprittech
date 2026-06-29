package tn.esprit.backend_esprittech.entity.projetcatalogue;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Projet complet déposé au catalogue applicatif par un enseignant affilié à une équipe
 * de recherche, après validation par le chef de cette équipe.
 *
 * <p>Les domaines, technologies et prérequis réutilisent les tables de référence existantes
 * ({@link SujetDomaine}, {@link SujetTechnologie}, {@link SujetPrerequis}) mais via des tables
 * de jointure DÉDIÉES ({@code projet_domaines}, {@code projet_technologies},
 * {@code projet_prerequis}) afin de ne pas interférer avec l'entité {@code SujetProjet}.</p>
 */
@Entity
@Table(name = "projet_catalogue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetCatalogue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le type de projet est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "type_projet", nullable = false, length = 30)
    private TypeProjet typeProjet;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    @Column(nullable = false, length = 200)
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Les objectifs sont obligatoires")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String objectifs;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @NotNull(message = "Le statut est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatutProjet statut = StatutProjet.SOUMIS_EN_VALIDATION;

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0;

    /** Enseignant qui dépose le projet (forcé côté serveur = utilisateur courant). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "encadrant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_projet_encadrant"))
    private User encadrant;

    /** Équipe de recherche déduite de l'affiliation acceptée de l'encadrant. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_projet_equipe"))
    private EquipeRecherche equipeRecherche;

    /** Chef d'équipe ayant validé ou refusé le projet (null tant qu'il est en attente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_validateur_id", foreignKey = @ForeignKey(name = "fk_projet_chef_validateur"))
    private User chefValidateur;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "projet_domaines",
            joinColumns = @JoinColumn(name = "projet_id"),
            inverseJoinColumns = @JoinColumn(name = "domaine_id")
    )
    private Set<SujetDomaine> domaines = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "projet_technologies",
            joinColumns = @JoinColumn(name = "projet_id"),
            inverseJoinColumns = @JoinColumn(name = "technologie_id")
    )
    private Set<SujetTechnologie> technologies = new LinkedHashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "projet_prerequis",
            joinColumns = @JoinColumn(name = "projet_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequis_id")
    )
    private Set<SujetPrerequis> prerequis = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (statut == null) {
            statut = StatutProjet.SOUMIS_EN_VALIDATION;
        }
        if (score == null) {
            score = 0;
        }
    }
}
