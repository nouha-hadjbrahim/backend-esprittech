package tn.esprit.backend_esprittech.repository.industrialisation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;

import java.util.List;

@Repository
public interface QuestionIndustrialisationRepository extends JpaRepository<QuestionIndustrialisation, Long> {

    List<QuestionIndustrialisation> findByActifTrueOrderByOrdreAsc();

    List<QuestionIndustrialisation> findAllByOrderByOrdreAsc();
}
