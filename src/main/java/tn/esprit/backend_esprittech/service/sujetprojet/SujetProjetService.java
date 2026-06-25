package tn.esprit.backend_esprittech.service.sujetprojet;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SujetProjetService {

    private static final List<StatutSujet> STATUTS_DISPONIBLES = List.of(
            StatutSujet.VALIDE,
            StatutSujet.CANDIDATURE_OUVERTE,
            StatutSujet.CANDIDATURE_FERMEE
    );

    private final SujetProjetRepository sujetProjetRepository;
    private final SujetProjetMapper sujetProjetMapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final SujetReferenceService referenceService;

    public SujetProjetService(SujetProjetRepository sujetProjetRepository,
                              SujetProjetMapper sujetProjetMapper,
                              UserService userService,
                              UserRepository userRepository,
                              SujetReferenceService referenceService) {
        this.sujetProjetRepository = sujetProjetRepository;
        this.sujetProjetMapper = sujetProjetMapper;
        this.userService = userService;
        this.userRepository = userRepository;
        this.referenceService = referenceService;
    }

    public SujetProjetResponse creerSujet(SujetProjetRequest request) {
        User encadrant = getCurrentEnseignant();
        SujetProjet sujet = sujetProjetMapper.toEntity(request, encadrant.getId());
        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getMesSujets(CategorieSujet categorie, StatutSujet statut) {
        Long encadrantId = getCurrentEnseignant().getId();
        List<SujetProjet> sujets;

        if (categorie != null && statut != null) {
            sujets = sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(encadrantId)
                    .stream()
                    .filter(s -> s.getCategorie() == categorie && s.getStatut() == statut)
                    .toList();
        } else if (categorie != null) {
            sujets = sujetProjetRepository.findByEncadrantIdAndCategorieOrderByDateCreationDesc(encadrantId, categorie);
        } else if (statut != null) {
            sujets = sujetProjetRepository.findByEncadrantIdAndStatutOrderByDateCreationDesc(encadrantId, statut);
        } else {
            sujets = sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(encadrantId);
        }

        return toResponses(sujets);
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getSujetsDisponibles(CategorieSujet categorie) {
        List<SujetProjet> sujets = sujetProjetRepository.findByStatutInOrderByDateCreationDesc(STATUTS_DISPONIBLES)
                .stream()
                .filter(SujetProjet::getCatalogue)
                .toList();

        if (categorie != null) {
            sujets = sujets.stream().filter(s -> s.getCategorie() == categorie).toList();
        }

        return toResponses(sujets);
    }

    @Transactional(readOnly = true)
    public SujetProjetResponse getSujetById(Long id) {
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));

        User current = userService.getCurrentUser();
        boolean isOwner = sujet.getEncadrantId().equals(current.getId());
        boolean isPublic = sujet.getCatalogue() && STATUTS_DISPONIBLES.contains(sujet.getStatut());

        if (!isOwner && !isPublic) {
            throw new AccessDeniedException("Accès refusé à ce sujet");
        }

        return toResponse(sujet);
    }

    public SujetProjetResponse modifierSujet(Long id, SujetProjetRequest request) {
        Long encadrantId = getCurrentEnseignant().getId();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(encadrantId))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetMapper.updateEntity(sujet, request);
        return toResponse(sujetProjetRepository.save(sujet));
    }

    public void supprimerSujet(Long id) {
        Long encadrantId = getCurrentEnseignant().getId();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(encadrantId))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetRepository.delete(sujet);
    }

    @Transactional(readOnly = true)
    public List<String> getTechnologiesDisponibles() {
        return referenceService.getAllTechnologieNoms();
    }

    @Transactional(readOnly = true)
    public List<String> getPrerequisSuggestions() {
        return referenceService.getAllPrerequisNoms();
    }

    @Transactional(readOnly = true)
    public List<String> getDomainesSuggestions() {
        return referenceService.getAllDomaineNoms();
    }

    private User getCurrentEnseignant() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ENSEIGNANT) {
            throw new AccessDeniedException("Seuls les enseignants peuvent déposer ou gérer des sujets");
        }
        return current;
    }

    private SujetProjetResponse toResponse(SujetProjet sujet) {
        String encadrantNom = userRepository.findById(sujet.getEncadrantId())
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse("Encadrant inconnu");
        return sujetProjetMapper.toResponse(sujet, encadrantNom);
    }

    private List<SujetProjetResponse> toResponses(List<SujetProjet> sujets) {
        Map<Long, String> noms = userRepository.findAllById(
                sujets.stream().map(SujetProjet::getEncadrantId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u.getPrenom() + " " + u.getNom()));

        return sujets.stream()
                .map(s -> sujetProjetMapper.toResponse(s, noms.getOrDefault(s.getEncadrantId(), "Encadrant inconnu")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getProjetsEvaluables() {
        List<SujetProjet> sujets = sujetProjetRepository.findByStatutOrderByDateCreationDesc(
                StatutSujet.REALISATION_TERMINEE
        );
        return sujets.stream()
                .map(sujetProjetMapper::toResponse)
                .toList();
    }
}
