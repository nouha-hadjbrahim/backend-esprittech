package tn.esprit.backend_esprittech.service.projetcatalogue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.CreateProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.request.RefuserProjetRequest;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCardResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCatalogueResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetDetailsResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ConflictException;
import tn.esprit.backend_esprittech.exception.ForbiddenException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.projetcatalogue.ProjetCatalogueMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetDomaineRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetPrerequisRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetTechnologieRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Logique métier du catalogue applicatif : dépôt d'un projet complet par un enseignant
 * affilié, validation/refus par le chef de son équipe, consultation des projets et du
 * catalogue public. Toutes les règles de sécurité fines (affiliation, chef de l'équipe,
 * encadrant) sont appliquées ici.
 */
@Service
@Transactional
public class ProjetCatalogueService {

    private final ProjetCatalogueRepository projetRepository;
    private final ProjetCatalogueMapper mapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final EquipeRechercheRepository equipeRepository;
    private final SujetDomaineRepository domaineRepository;
    private final SujetTechnologieRepository technologieRepository;
    private final SujetPrerequisRepository prerequisRepository;

    public ProjetCatalogueService(ProjetCatalogueRepository projetRepository,
                                  ProjetCatalogueMapper mapper,
                                  UserService userService,
                                  UserRepository userRepository,
                                  EquipeRechercheRepository equipeRepository,
                                  SujetDomaineRepository domaineRepository,
                                  SujetTechnologieRepository technologieRepository,
                                  SujetPrerequisRepository prerequisRepository) {
        this.projetRepository = projetRepository;
        this.mapper = mapper;
        this.userService = userService;
        this.userRepository = userRepository;
        this.equipeRepository = equipeRepository;
        this.domaineRepository = domaineRepository;
        this.technologieRepository = technologieRepository;
        this.prerequisRepository = prerequisRepository;
    }

    // -------------------------------------------------------------------------
    // Dépôt d'un projet (enseignant affilié)
    // -------------------------------------------------------------------------

    public ProjetCatalogueResponse creerProjet(CreateProjetRequest request) {
        User encadrant = currentEnseignantManaged();

        EquipeRecherche equipe = encadrant.getEquipeRecherche();
        if (equipe == null) {
            throw new ForbiddenException(
                    "Vous devez être affilié à une équipe de recherche pour soumettre un projet");
        }

        if (!request.dateDebut().isBefore(request.dateFin())) {
            throw new BusinessException("La date de début doit être antérieure à la date de fin");
        }

        ProjetCatalogue projet = ProjetCatalogue.builder()
                .typeProjet(request.typeProjet())
                .titre(request.titre().trim())
                .description(request.description().trim())
                .objectifs(request.objectifs().trim())
                .dateDebut(request.dateDebut())
                .dateFin(request.dateFin())
                .statut(StatutProjet.SOUMIS_EN_VALIDATION)
                .score(0)
                .encadrant(encadrant)
                .equipeRecherche(equipe)
                .domaines(resolveDomaines(request.domainesIds()))
                .technologies(resolveTechnologies(request.technologiesIds()))
                .prerequis(resolvePrerequis(request.prerequisIds()))
                .build();

        return mapper.toResponse(projetRepository.save(projet));
    }

    @Transactional(readOnly = true)
    public List<ProjetCardResponse> mesProjets() {
        User encadrant = currentEnseignant();
        return projetRepository.findByEncadrantIdOrderByDateCreationDesc(encadrant.getId())
                .stream().map(mapper::toCard).toList();
    }

    // -------------------------------------------------------------------------
    // Validation / refus (chef d'équipe)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjetCardResponse> projetsAValider() {
        User chef = currentChef();
        EquipeRecherche equipe = equipeRepository.findByChefId(chef.getId()).orElse(null);
        if (equipe == null) {
            return List.of();
        }
        return projetRepository
                .findByEquipeRechercheIdAndStatutOrderByDateCreationDesc(equipe.getId(),
                        StatutProjet.SOUMIS_EN_VALIDATION)
                .stream().map(mapper::toCard).toList();
    }

    public ProjetCatalogueResponse valider(Long id) {
        User chef = currentChef();
        ProjetCatalogue projet = getProjetOrThrow(id);
        verifierChefDeLEquipe(projet, chef);
        verifierEnAttente(projet);

        projet.setStatut(StatutProjet.VALIDE);
        projet.setChefValidateur(chef);
        projet.setDateValidation(LocalDateTime.now());
        projet.setMotifRefus(null);
        return mapper.toResponse(projetRepository.save(projet));
    }

    public ProjetCatalogueResponse refuser(Long id, RefuserProjetRequest request) {
        User chef = currentChef();
        ProjetCatalogue projet = getProjetOrThrow(id);
        verifierChefDeLEquipe(projet, chef);
        verifierEnAttente(projet);

        projet.setStatut(StatutProjet.INVALIDE);
        projet.setMotifRefus(request.motifRefus().trim());
        projet.setChefValidateur(chef);
        projet.setDateValidation(LocalDateTime.now());
        return mapper.toResponse(projetRepository.save(projet));
    }

    // -------------------------------------------------------------------------
    // Catalogue public + détails
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjetCardResponse> catalogue(TypeProjet type, Long domaineId, Integer annee, String search) {
        String terme = (search != null && !search.isBlank()) ? search.trim() : null;
        return projetRepository
                .rechercherCatalogue(StatutProjet.STATUTS_CATALOGUE, type, domaineId, annee, terme)
                .stream().map(mapper::toCard).toList();
    }

    @Transactional(readOnly = true)
    public ProjetDetailsResponse detailsCatalogue(Long id) {
        ProjetCatalogue projet = getProjetOrThrow(id);
        if (!StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())) {
            throw new ResourceNotFoundException("Projet introuvable dans le catalogue (id=" + id + ")");
        }
        return mapper.toDetails(projet);
    }

    @Transactional(readOnly = true)
    public ProjetDetailsResponse detailsProjet(Long id) {
        User current = userService.getCurrentUser();
        ProjetCatalogue projet = getProjetOrThrow(id);

        boolean estEncadrant = projet.getEncadrant() != null
                && projet.getEncadrant().getId().equals(current.getId());
        boolean estChefEquipe = projet.getEquipeRecherche() != null
                && projet.getEquipeRecherche().getChef() != null
                && projet.getEquipeRecherche().getChef().getId().equals(current.getId());
        boolean estAdmin = current.getRole() == Role.ROLE_ADMIN;

        if (!estEncadrant && !estChefEquipe && !estAdmin) {
            throw new ForbiddenException("Vous n'avez pas accès à ce projet");
        }
        return mapper.toDetails(projet);
    }

    // -------------------------------------------------------------------------
    // Références pour le formulaire (domaines / technologies / prérequis)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> getDomaines() {
        return domaineRepository.findAllByOrderByNomAsc().stream()
                .map(d -> new ReferenceItemResponse(d.getId(), d.getNom(), d.getDateCreation()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> getTechnologies() {
        return technologieRepository.findAllByOrderByNomAsc().stream()
                .map(t -> new ReferenceItemResponse(t.getId(), t.getNom(), t.getDateCreation()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReferenceItemResponse> getPrerequis() {
        return prerequisRepository.findAllByOrderByNomAsc().stream()
                .map(p -> new ReferenceItemResponse(p.getId(), p.getNom(), p.getDateCreation()))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private ProjetCatalogue getProjetOrThrow(Long id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable (id=" + id + ")"));
    }

    /** Utilisateur courant, doit être enseignant (principal de sécurité, non rechargé). */
    private User currentEnseignant() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ENSEIGNANT) {
            throw new ForbiddenException("Seuls les enseignants peuvent soumettre ou consulter leurs projets");
        }
        return current;
    }

    /** Enseignant courant rechargé (entité gérée) pour accéder à son équipe et le persister. */
    private User currentEnseignantManaged() {
        User current = currentEnseignant();
        return userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    /** Chef d'équipe courant rechargé (entité gérée). */
    private User currentChef() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new ForbiddenException("Seul un chef d'équipe peut effectuer cette action");
        }
        return userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private void verifierChefDeLEquipe(ProjetCatalogue projet, User chef) {
        EquipeRecherche equipe = projet.getEquipeRecherche();
        User chefEquipe = (equipe != null) ? equipe.getChef() : null;
        if (chefEquipe == null || !chefEquipe.getId().equals(chef.getId())) {
            throw new ForbiddenException("Vous ne pouvez valider que les projets de votre équipe");
        }
    }

    private void verifierEnAttente(ProjetCatalogue projet) {
        if (projet.getStatut() != StatutProjet.SOUMIS_EN_VALIDATION) {
            throw new ConflictException("Ce projet n'est pas en attente de validation");
        }
    }

    private Set<SujetDomaine> resolveDomaines(List<Long> ids) {
        Set<Long> uniques = new LinkedHashSet<>(ids);
        List<SujetDomaine> trouves = domaineRepository.findAllById(uniques);
        if (trouves.size() != uniques.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs domaines sont introuvables");
        }
        return new LinkedHashSet<>(trouves);
    }

    private Set<SujetTechnologie> resolveTechnologies(List<Long> ids) {
        Set<Long> uniques = new LinkedHashSet<>(ids);
        List<SujetTechnologie> trouves = technologieRepository.findAllById(uniques);
        if (trouves.size() != uniques.size()) {
            throw new ResourceNotFoundException("Une ou plusieurs technologies sont introuvables");
        }
        return new LinkedHashSet<>(trouves);
    }

    private Set<SujetPrerequis> resolvePrerequis(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Long> uniques = new LinkedHashSet<>(ids);
        List<SujetPrerequis> trouves = prerequisRepository.findAllById(uniques);
        if (trouves.size() != uniques.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs prérequis sont introuvables");
        }
        return new LinkedHashSet<>(trouves);
    }
}
