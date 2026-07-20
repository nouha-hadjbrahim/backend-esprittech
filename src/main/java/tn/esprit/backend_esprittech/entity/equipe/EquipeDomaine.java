package tn.esprit.backend_esprittech.entity.equipe;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipe_domaines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipeDomaine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String nom;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
