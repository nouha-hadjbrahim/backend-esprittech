package tn.esprit.backend_esprittech.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.entity.equipe.EquipeDomaine;
import tn.esprit.backend_esprittech.repository.equipe.EquipeDomaineRepository;

import java.util.List;

@Component
@Order(3)
public class EquipeDomaineDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EquipeDomaineDataInitializer.class);

    private final EquipeDomaineRepository domaineRepository;

    public EquipeDomaineDataInitializer(EquipeDomaineRepository domaineRepository) {
        this.domaineRepository = domaineRepository;
    }

    @Override
    public void run(String... args) {
        if (domaineRepository.count() > 0) {
            log.info("Référentiel équipe domaines déjà présent, initialisation ignorée.");
            return;
        }

        List<String> defaults = List.of(
                "Informatique", "Réseaux", "Cloud & DevOps", "Cybersécurité",
                "Data Science", "IoT", "Robotique", "Génie Logiciel",
                "Intelligence Artificielle", "Systèmes Embarqués"
        );

        defaults.stream()
                .map(nom -> EquipeDomaine.builder().nom(nom).build())
                .forEach(domaineRepository::save);

        log.info("Référentiel équipe domaines initialisé ({} entrées).", defaults.size());
    }
}
