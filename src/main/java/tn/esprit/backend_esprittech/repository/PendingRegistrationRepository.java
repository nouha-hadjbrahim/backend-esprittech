package tn.esprit.backend_esprittech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.PendingRegistration;

import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);
}
