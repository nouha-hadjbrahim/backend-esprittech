package tn.esprit.backend_esprittech.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetDomaineRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetPrerequisRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetTechnologieRepository;

import java.util.List;

@Component
@Order(2)
public class SujetReferenceDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SujetReferenceDataInitializer.class);

    private final SujetDomaineRepository domaineRepository;
    private final SujetPrerequisRepository prerequisRepository;
    private final SujetTechnologieRepository technologieRepository;

    public SujetReferenceDataInitializer(SujetDomaineRepository domaineRepository,
                                           SujetPrerequisRepository prerequisRepository,
                                           SujetTechnologieRepository technologieRepository) {
        this.domaineRepository = domaineRepository;
        this.prerequisRepository = prerequisRepository;
        this.technologieRepository = technologieRepository;
    }

    @Override
    public void run(String... args) {
        seedDomaines();
        seedPrerequis();
        seedTechnologies();
    }

    private void seedDomaines() {
        if (domaineRepository.count() > 0) {
            return;
        }
        List<String> defaults = List.of(
                "Intelligence Artificielle", "Cloud & DevOps", "Génie Logiciel", "Data Science",
                "IoT", "Réseaux", "Robotique", "Cybersécurité"
        );
        defaults.forEach(nom -> domaineRepository.save(SujetDomaine.builder().nom(nom).build()));
        log.info("Référentiel domaines initialisé ({} entrées).", defaults.size());
    }

    private void seedPrerequis() {
        if (prerequisRepository.count() > 0) {
            return;
        }
        List<String> defaults = List.of(
                "Soft skills", "Hard skills", "Travail en équipe", "Autonomie", "Communication", "Gestion de projet"
        );
        defaults.forEach(nom -> prerequisRepository.save(SujetPrerequis.builder().nom(nom).build()));
        log.info("Référentiel prérequis initialisé ({} entrées).", defaults.size());
    }

    private void seedTechnologies() {
        if (technologieRepository.count() > 0) {
            return;
        }
        List<String> defaults = List.of(
                "Java", "Python", "Angular", "Spring Boot", "React", "Docker",
                "Kubernetes", "PostgreSQL", "TensorFlow", "Node.js"
        );
        defaults.forEach(nom -> technologieRepository.save(SujetTechnologie.builder().nom(nom).build()));
        log.info("Référentiel technologies initialisé ({} entrées).", defaults.size());
    }
}
