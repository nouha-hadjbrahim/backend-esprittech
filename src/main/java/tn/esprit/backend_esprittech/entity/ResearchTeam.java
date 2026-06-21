package tn.esprit.backend_esprittech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "research_teams", uniqueConstraints = {
    @UniqueConstraint(name = "uk_research_teams_domaine_recherche", columnNames = "domaine_recherche")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String domaineRecherche;

    @Column(nullable = false)
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false, unique = true)
    private User leader;

    @Builder.Default
    @OneToMany(mappedBy = "researchTeam", fetch = FetchType.LAZY)
    private List<User> members = new ArrayList<>();

    public void addMember(User user) {
        if (user == null) {
            return;
        }
        members.add(user);
        user.setResearchTeam(this);
    }

    public void removeMember(User user) {
        if (user == null) {
            return;
        }
        members.remove(user);
        if (user.getResearchTeam() == this) {
            user.setResearchTeam(null);
        }
    }
}