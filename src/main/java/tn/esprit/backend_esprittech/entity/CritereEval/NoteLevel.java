package tn.esprit.backend_esprittech.entity.CritereEval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "evaluation_note_levels",
        uniqueConstraints = @UniqueConstraint(name = "uk_note_level_value", columnNames = "level_value")
)
public class NoteLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_value", nullable = false)
    private Integer value;

    @Column(nullable = false)
    private String label;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "display_order", nullable = false)
    private Integer orderIndex;

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
        if (orderIndex == null) {
            orderIndex = value;
        }
    }

    @PreUpdate
    void onUpdate() {
        dateMiseAJour = LocalDateTime.now();
    }
}
