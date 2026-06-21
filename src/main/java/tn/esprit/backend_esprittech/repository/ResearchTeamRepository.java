package tn.esprit.backend_esprittech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.ResearchTeam;

@Repository
public interface ResearchTeamRepository extends JpaRepository<ResearchTeam, Long> {

    boolean existsByDomaineRecherche(String domaineRecherche);

    boolean existsByDomaineRechercheAndIdNot(String domaineRecherche, Long id);

    boolean existsByLeaderId(Long leaderId);

    boolean existsByLeaderIdAndIdNot(Long leaderId, Long id);
}