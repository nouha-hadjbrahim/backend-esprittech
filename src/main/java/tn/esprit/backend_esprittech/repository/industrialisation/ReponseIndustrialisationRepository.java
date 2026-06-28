package tn.esprit.backend_esprittech.repository.industrialisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReponseIndustrialisationRepository extends JpaRepository<ReponseIndustrialisation, Long> {

    List<ReponseIndustrialisation> findByCandidatureIndustrialisationId(Long candidatureId);

    Optional<ReponseIndustrialisation> findByCandidatureIndustrialisationIdAndQuestionId(Long candidatureId, Long questionId);
}
