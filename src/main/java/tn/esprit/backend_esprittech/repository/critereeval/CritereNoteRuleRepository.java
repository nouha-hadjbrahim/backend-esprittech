package tn.esprit.backend_esprittech.repository.critereeval;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.critereeval.CritereNoteRule;

import java.util.List;
import java.util.Optional;

public interface CritereNoteRuleRepository extends JpaRepository<CritereNoteRule, Long> {

    List<CritereNoteRule> findByCritereNoteIdOrderByPriorityAscIdAsc(Long critereNoteId);

    List<CritereNoteRule> findByCritereNoteIdAndActiveTrueOrderByPriorityAscIdAsc(Long critereNoteId);

    boolean existsByCritereNoteId(Long critereNoteId);

    boolean existsByNoteValue(Integer noteValue);

    Optional<CritereNoteRule> findByCritereNoteIdAndRuleNameIgnoreCase(Long critereNoteId, String ruleName);
}
