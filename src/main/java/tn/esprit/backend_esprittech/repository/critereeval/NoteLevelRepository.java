package tn.esprit.backend_esprittech.repository.critereeval;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.critereeval.NoteLevel;

import java.util.List;
import java.util.Optional;

public interface NoteLevelRepository extends JpaRepository<NoteLevel, Long> {

    List<NoteLevel> findByActiveTrueOrderByOrderIndexAsc();

    List<NoteLevel> findAllByOrderByOrderIndexAsc();

    Optional<NoteLevel> findByValue(Integer value);

    Optional<NoteLevel> findByValueAndActiveTrue(Integer value);

    Optional<NoteLevel> findByOrderIndex(Integer orderIndex);

    Optional<NoteLevel> findTopByActiveTrueOrderByValueDesc();

    boolean existsByValue(Integer value);
}
