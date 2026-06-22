package tn.esprit.backend_esprittech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.ResearchTeam;

public interface ResearchTeamRepository extends JpaRepository<ResearchTeam, Long> {

    boolean existsByNom(String nom);

    boolean existsByNomAndIdNot(String nom, Long id);

    boolean existsByLeaderId(Long leaderId);

    boolean existsByLeaderIdAndIdNot(Long leaderId, Long id);
}