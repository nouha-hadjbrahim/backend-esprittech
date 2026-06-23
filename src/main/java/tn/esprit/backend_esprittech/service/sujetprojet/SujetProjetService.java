package tn.esprit.backend_esprittech.service.sujetprojet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.MockEncadrantResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class SujetProjetService {

    private static final List<String> TECHNOLOGIES_DISPONIBLES = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "Angular", "React", "Vue.js",
            "Spring Boot", "Node.js", "Express.js", "Django", "Flask", "FastAPI",
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Docker", "Kubernetes",
            "AWS", "Azure", "GCP", "Git", "CI/CD", "DevOps",
            "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch", "Scikit-learn",
            "REST API", "GraphQL", "Microservices", "Flutter", "Android", "iOS",
            "Linux", "Bash", "C++", "C#", ".NET", "PHP", "Laravel", "Symfony"
    );

    private final SujetProjetRepository sujetProjetRepository;
    private final SujetProjetMapper sujetProjetMapper;

    public SujetProjetService(SujetProjetRepository sujetProjetRepository,
                              SujetProjetMapper sujetProjetMapper) {
        this.sujetProjetRepository = sujetProjetRepository;
        this.sujetProjetMapper = sujetProjetMapper;
    }

    public SujetProjetResponse creerSujet(SujetProjetRequest request) {
        SujetProjet sujet = sujetProjetMapper.toEntity(request);
        return sujetProjetMapper.toResponse(sujetProjetRepository.save(sujet));
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getMesSujets(CategorieSujet categorie, StatutSujet statut) {
        List<SujetProjet> sujets;

        if (categorie != null && statut != null) {
            sujets = sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(MockEncadrantContext.ENCADRANT_ID)
                    .stream()
                    .filter(s -> s.getCategorie() == categorie && s.getStatut() == statut)
                    .toList();
        } else if (categorie != null) {
            sujets = sujetProjetRepository.findByEncadrantIdAndCategorieOrderByDateCreationDesc(
                    MockEncadrantContext.ENCADRANT_ID, categorie);
        } else if (statut != null) {
            sujets = sujetProjetRepository.findByEncadrantIdAndStatutOrderByDateCreationDesc(
                    MockEncadrantContext.ENCADRANT_ID, statut);
        } else {
            sujets = sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(MockEncadrantContext.ENCADRANT_ID);
        }

        return sujets.stream()
                .map(sujetProjetMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SujetProjetResponse getSujetById(Long id) {
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(MockEncadrantContext.ENCADRANT_ID))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        return sujetProjetMapper.toResponse(sujet);
    }

    public SujetProjetResponse modifierSujet(Long id, SujetProjetRequest request) {
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(MockEncadrantContext.ENCADRANT_ID))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetMapper.updateEntity(sujet, request);
        return sujetProjetMapper.toResponse(sujetProjetRepository.save(sujet));
    }

    @Transactional(readOnly = true)
    public List<String> getTechnologiesDisponibles() {
        return TECHNOLOGIES_DISPONIBLES.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Transactional(readOnly = true)
    public MockEncadrantResponse getMockEncadrant() {
        return new MockEncadrantResponse(
                MockEncadrantContext.ENCADRANT_ID,
                MockEncadrantContext.NOM_COMPLET,
                MockEncadrantContext.ROLE,
                MockEncadrantContext.EMAIL,
                MockEncadrantContext.DEPARTEMENT
        );
    }
}
