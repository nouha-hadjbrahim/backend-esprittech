package tn.esprit.backend_esprittech.entity.critereeval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "critere_note_rules")
public class CritereNoteRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "critere_note_id", nullable = false)
    private CritereNote critereNote;

    @Column(nullable = false)
    private String ruleName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String metadataKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleOperator operator;

    @Column(length = 1000)
    private String expectedValue;

    private Double minValue;

    private Double maxValue;

    @Column(nullable = false)
    private Integer noteValue;

    @Column(name = "priority_order", nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateMiseAJour;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        dateCreation = now;
        dateMiseAJour = now;
        if (active == null) {
            active = true;
        }
        if (priority == null) {
            priority = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        dateMiseAJour = LocalDateTime.now();
    }
}
