package tn.esprit.backend_esprittech.repository.critereeval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.critereeval.CritereEliminatoire;

import java.util.List;

public interface CritereEliminatoireRepository extends JpaRepository<CritereEliminatoire, Long> {

    List<CritereEliminatoire> findByActifTrueOrderByOrdreAsc();
}