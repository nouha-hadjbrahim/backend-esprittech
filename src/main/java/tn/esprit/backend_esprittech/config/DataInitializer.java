package tn.esprit.backend_esprittech.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.UserRepository;

/**
 * Crée un compte admin par défaut au démarrage s'il n'existe pas encore.
 * Les identifiants par défaut sont configurables via .env (ADMIN_EMAIL / ADMIN_PASSWORD).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email:admin@esprittech.tn}") String adminEmail,
                           @Value("${app.admin.password:Admin@1234}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Compte admin déjà présent ({}), initialisation ignorée.", adminEmail);
            return;
        }

        User admin = User.builder()
                .nom("Admin")
                .prenom("EspritTECH")
                .email(adminEmail)
                .identifiant("ADMIN-001")
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ROLE_ADMIN)
                .typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                .departement("Administration")
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.warn("Compte admin par défaut créé : {} / (mot de passe défini via ADMIN_PASSWORD). "
                + "Pensez à le changer en production.", adminEmail);
    }
}
