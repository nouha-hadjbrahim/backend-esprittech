package tn.esprit.backend_esprittech.service.projetcatalogue;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
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
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.NotificationType;
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
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetDomaineRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetPrerequisRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetTechnologieRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;

import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetReferenceService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private static final String TYPE_IMAGE_JPEG = "image/jpeg";


    private final ProjetCatalogueRepository projetRepository;
    private final ProjetCatalogueMapper mapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final EquipeRechercheRepository equipeRepository;
    private final SujetDomaineRepository domaineRepository;
    private final SujetTechnologieRepository technologieRepository;
    private final SujetPrerequisRepository prerequisRepository;
    private final LivrableCatalogueRepository livrableCatalogueRepository;
    private final SujetReferenceService sujetReferenceService;
    private final NotificationService notificationService;

    public ProjetCatalogueService(ProjetCatalogueRepository projetRepository,
                                  ProjetCatalogueMapper mapper,
                                  UserService userService,
                                  UserRepository userRepository,
                                  EquipeRechercheRepository equipeRepository,
                                  SujetDomaineRepository domaineRepository,
                                  SujetTechnologieRepository technologieRepository,
                                  SujetPrerequisRepository prerequisRepository,
                                  LivrableCatalogueRepository livrableCatalogueRepository,
                                  SujetReferenceService sujetReferenceService,
                                  NotificationService notificationService) {
        this.projetRepository = projetRepository;
        this.mapper = mapper;
        this.userService = userService;
        this.userRepository = userRepository;
        this.equipeRepository = equipeRepository;
        this.domaineRepository = domaineRepository;
        this.technologieRepository = technologieRepository;
        this.prerequisRepository = prerequisRepository;
        this.livrableCatalogueRepository = livrableCatalogueRepository;
        this.sujetReferenceService = sujetReferenceService;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------------
    // Dépôt d'un projet (enseignant affilié)
    // -------------------------------------------------------------------------

    @Historise(action = ActionType.CREATE, entityType = EntityType.PROJET_CATALOGUE, entityIdExpression = "#result.id()", summaryExpression = "'Création projet catalogue ' + #result.titre()")
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

        applyCover(projet, request.coverImageBase64(), request.coverImageContentType());

        ProjetCatalogue saved;
        try {
            saved = projetRepository.save(projet);
        } catch (DataAccessException ex) {
            if (isPacketTooLarge(ex)) {
                throw new BusinessException(
                        "L'image de couverture est trop volumineuse pour être enregistrée (max " + MAX_COVER_LABEL + "). "
                                + "Choisissez une image plus légère.");
            }
            throw ex;
        }

        notifierChefNouveauProjet(saved, encadrant, equipe);

        return mapper.toResponse(saved);
    }

    /**
     * Notifie le chef de l'équipe qu'un nouveau projet vient d'être soumis pour validation.
     * Aucune notification si l'équipe n'a pas de chef ou si l'encadrant est lui-même le chef.
     */
    private void notifierChefNouveauProjet(ProjetCatalogue projet, User encadrant, EquipeRecherche equipe) {
        User chef = equipe.getChef();
        if (chef == null || chef.getId().equals(encadrant.getId())) {
            return;
        }
        notificationService.creerNotification(
                chef.getId(),
                NotificationType.PROJET_SOUMIS,
                "Nouveau projet a valider",
                "L'enseignant " + encadrant.getPrenom() + " " + encadrant.getNom()
                        + " a soumis un nouveau projet \"" + projet.getTitre() + "\" a valider.",
                "/frontoffice/validation-projets");
    }

    /**
     * Publie automatiquement un sujet terminé dans le catalogue public (statut VALIDE),
     * sans passer par la validation chef. Ne modifie pas le flux de dépôt manuel.
     */
    public ProjetCatalogueResponse publierDepuisSujetTermine(SujetProjet sujet,
                                                            String coverImageBase64,
                                                            String coverImageContentType) {
        if (sujet == null || sujet.getId() == null) {
            throw new BusinessException("Sujet introuvable pour publication au catalogue.");
        }
        if (projetRepository.existsBySujetId(sujet.getId())) {
            return mapper.toResponse(projetRepository.findBySujetId(sujet.getId())
                    .orElseThrow(() -> new ConflictException("Ce sujet est déjà publié au catalogue.")));
        }

        User encadrant = userRepository.findById(sujet.getEncadrantId())
                .orElseThrow(() -> new ResourceNotFoundException("Encadrant introuvable"));
        EquipeRecherche equipe = encadrant.getEquipeRecherche();
        if (equipe == null) {
            equipe = equipeRepository.findByChefId(encadrant.getId()).orElse(null);
        }
        if (equipe == null) {
            throw new ForbiddenException(
                    "L'encadrant doit être affilié à une équipe pour publier le sujet au catalogue.");
        }

        User chef = equipe.getChef();
        LocalDate dateDebut = resolveDateDebut(sujet);
        LocalDate dateFin = sujet.getDateTerminaison() != null
                ? sujet.getDateTerminaison().toLocalDate()
                : LocalDate.now();
        if (!dateDebut.isBefore(dateFin)) {
            dateDebut = dateFin.minusDays(1);
        }

        int score = sujet.getScoreFinalEvaluation() != null
                ? (int) Math.round(sujet.getScoreFinalEvaluation())
                : 0;

        ProjetCatalogue projet = ProjetCatalogue.builder()
                .sujetId(sujet.getId())
                .typeProjet(mapTypeProjet(sujet.getCategorie()))
                .titre(sujet.getTitre().trim())
                .description(sujet.getDescription().trim())
                .objectifs(sujet.getObjectifs().trim())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .statut(StatutProjet.VALIDE)
                .score(score)
                .encadrant(encadrant)
                .equipeRecherche(equipe)
                .chefValidateur(chef)
                .motifRefus(null)
                .dateValidation(LocalDateTime.now())
                .domaines(resolveDomainesByNoms(sujet.getDomaine()))
                .technologies(resolveTechnologiesByNoms(sujet.getTechnologies()))
                .prerequis(resolvePrerequisByNoms(sujet.getPrerequis()))
                .build();

        applyCover(projet, coverImageBase64, coverImageContentType);

        try {
            return mapper.toResponse(projetRepository.save(projet));
        } catch (DataAccessException ex) {
            if (isPacketTooLarge(ex)) {
                throw new BusinessException(
                        "L'image de couverture est trop volumineuse pour être enregistrée (max " + MAX_COVER_LABEL + "). "
                                + "Choisissez une image plus légère.");
            }
            throw ex;
        }
    }

    private LocalDate resolveDateDebut(SujetProjet sujet) {
        if (sujet.getDateDebutRealisation() != null) {
            return sujet.getDateDebutRealisation().toLocalDate();
        }
        if (sujet.getDateValidation() != null) {
            return sujet.getDateValidation().toLocalDate();
        }
        if (sujet.getDateCreation() != null) {
            return sujet.getDateCreation().toLocalDate();
        }
        return LocalDate.now().minusMonths(1);
    }

    private TypeProjet mapTypeProjet(CategorieSujet categorie) {
        if (categorie == null) {
            return TypeProjet.PFE;
        }
        return switch (categorie) {
            case RDI -> TypeProjet.RDI;
            case STAGE_INGENIEUR -> TypeProjet.STAGE_INGENIEUR;
            case PFE, PROJET_COMPLET -> TypeProjet.PFE;
        };
    }

    private Set<SujetDomaine> resolveDomainesByNoms(String csv) {
        Set<SujetDomaine> result = new LinkedHashSet<>();
        for (String nom : splitCsv(csv)) {
            Long id = sujetReferenceService.findOrCreateDomaine(nom).id();
            domaineRepository.findById(id).ifPresent(result::add);
        }
        if (result.isEmpty()) {
            throw new BusinessException("Le sujet doit avoir au moins un domaine pour être publié au catalogue.");
        }
        return result;
    }

    private Set<SujetTechnologie> resolveTechnologiesByNoms(String csv) {
        Set<SujetTechnologie> result = new LinkedHashSet<>();
        for (String nom : splitCsv(csv)) {
            Long id = sujetReferenceService.findOrCreateTechnologie(nom).id();
            technologieRepository.findById(id).ifPresent(result::add);
        }
        if (result.isEmpty()) {
            throw new BusinessException("Le sujet doit avoir au moins une technologie pour être publié au catalogue.");
        }
        return result;
    }

    private Set<SujetPrerequis> resolvePrerequisByNoms(String csv) {
        Set<SujetPrerequis> result = new LinkedHashSet<>();
        for (String nom : splitCsv(csv)) {
            Long id = sujetReferenceService.findOrCreatePrerequis(nom).id();
            prerequisRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
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

    public String getTitreById(Long id) {
        return projetRepository.findById(id)
                .map(ProjetCatalogue::getTitre)
                .orElse("Inconnu");
    }

    public Map<String, Object> getOldValues(Long id) {
        ProjetCatalogue p = getProjetOrThrow(id);
        Map<String, Object> map = new HashMap<>();
        map.put("statut", p.getStatut());
        return map;
    }

    @Historise(action = ActionType.ACCEPT, entityType = EntityType.PROJET_CATALOGUE, entityIdExpression = "#id", oldValuesExpression = "@projetCatalogueService.getOldValues(#id)", newValuesExpression = "{'statut': 'VALIDE'}", summaryExpression = "'Validation projet catalogue ' + @projetCatalogueService.getTitreById(#id)")
    public ProjetCatalogueResponse valider(Long id) {
        User chef = currentChef();
        ProjetCatalogue projet = getProjetOrThrow(id);
        verifierChefDeLEquipe(projet, chef);
        verifierEnAttente(projet);

        projet.setStatut(StatutProjet.VALIDE);
        projet.setChefValidateur(chef);
        projet.setDateValidation(LocalDateTime.now());
        projet.setMotifRefus(null);
        ProjetCatalogueResponse response = mapper.toResponse(projetRepository.save(projet));

        notificationService.creerNotification(
                projet.getEncadrant().getId(),
                NotificationType.PROJET_VALIDE,
                "Projet valide",
                "Votre projet \"" + projet.getTitre() + "\" a ete valide par le chef d'equipe.",
                "/frontoffice/mes-projets");

        return response;
    }

    @Historise(action = ActionType.REFUSE, entityType = EntityType.PROJET_CATALOGUE, entityIdExpression = "#id", oldValuesExpression = "@projetCatalogueService.getOldValues(#id)", metadataExpression = "{'motifRefus': #request.motifRefus()}", summaryExpression = "'Refus projet catalogue ' + @projetCatalogueService.getTitreById(#id)")
    public ProjetCatalogueResponse refuser(Long id, RefuserProjetRequest request) {
        User chef = currentChef();
        ProjetCatalogue projet = getProjetOrThrow(id);
        verifierChefDeLEquipe(projet, chef);
        verifierEnAttente(projet);

        projet.setStatut(StatutProjet.INVALIDE);
        projet.setMotifRefus(request.motifRefus().trim());
        projet.setChefValidateur(chef);
        projet.setDateValidation(LocalDateTime.now());
        ProjetCatalogueResponse response = mapper.toResponse(projetRepository.save(projet));

        notificationService.creerNotification(
                projet.getEncadrant().getId(),
                NotificationType.PROJET_REJETE,
                "Projet refuse",
                "Votre projet \"" + projet.getTitre() + "\" a ete refuse. Motif : " + request.motifRefus(),
                "/frontoffice/mes-projets");

        return response;
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
    // Vue admin : tous les projets
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjetCardResponse> tousLesProjets() {
        return projetRepository.findAllByOrderByDateCreationDesc().stream().map(mapper::toCard).toList();
    }

    // -------------------------------------------------------------------------
    // Suppression (admin)
    // -------------------------------------------------------------------------

    @Historise(action = ActionType.DELETE, entityType = EntityType.PROJET_CATALOGUE, entityIdExpression = "#id", oldValuesExpression = "@projetCatalogueService.getOldValues(#id)", summaryExpression = "'Suppression projet catalogue ' + @projetCatalogueService.getTitreById(#id)")
    public void supprimerProjetAsAdmin(Long id) {
        ProjetCatalogue projet = getProjetOrThrow(id);
        livrableCatalogueRepository.deleteAll(livrableCatalogueRepository.findByProjetId(id));
        projetRepository.delete(projet);
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

    /** Types MIME autorisés pour l'image de couverture. */
    private static final Set<String> ALLOWED_COVER_TYPES = Set.of("image/png", TYPE_IMAGE_JPEG, "image/jpg");

    /**
     * Taille maximale (octets décodés) de l'image de couverture.
     * Alignée sur spring.servlet.multipart.max-file-size (5 Mo).
     */
    private static final int MAX_COVER_BYTES = 5 * 1024 * 1024;

    /** Libellé de la taille maximale, réutilisé dans les messages d'erreur. */
    private static final String MAX_COVER_LABEL = "5 Mo";

    /**
     * Décode l'image de couverture (base64 ou data URI) et l'attache au projet après
     * validation du type et de la taille. Sans image fournie, aucune modification.
     */
    private void applyCover(ProjetCatalogue projet, String base64, String contentType) {
        if (base64 == null || base64.isBlank()) {
            return;
        }
        int commaIndex = base64.indexOf(',');
        String data = commaIndex >= 0 ? base64.substring(commaIndex + 1) : base64;
        // Nettoyer les retours à la ligne éventuels du base64
        data = data.replaceAll("\\s", "");
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Image de couverture invalide.");
        }
        if (bytes.length == 0) {
            return;
        }
        if (bytes.length > MAX_COVER_BYTES) {
            throw new BusinessException("L'image de couverture est trop volumineuse (max " + MAX_COVER_LABEL + ").");
        }
        String type = contentType != null ? contentType.trim().toLowerCase(java.util.Locale.ROOT) : null;
        if (type != null && type.contains(";")) {
            type = type.substring(0, type.indexOf(';')).trim();
        }
        if (type == null || type.isBlank() || !ALLOWED_COVER_TYPES.contains(type)) {
            type = detectImageContentType(bytes);
        }
        if (type == null || !ALLOWED_COVER_TYPES.contains(type)) {
            throw new BusinessException("Format d'image non autorisé. Formats acceptés : PNG, JPG.");
        }
        projet.setCoverImage(bytes);
        projet.setCoverImageContentType("image/jpg".equals(type) ? TYPE_IMAGE_JPEG : type);
    }

    private String detectImageContentType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return TYPE_IMAGE_JPEG;
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "image/png";
        }
        return null;
    }

    /** Met à jour le score d'un projet catalogue publié depuis un sujet terminé. */
    public void mettreAJourScoreParSujetId(Long sujetId, Integer score) {
        if (sujetId == null || score == null) {
            return;
        }
        projetRepository.findBySujetId(sujetId).ifPresent(projet -> {
            projet.setScore(Math.max(0, score));
            projetRepository.save(projet);
        });
    }

    /** Détecte l'erreur MySQL « packet too large » (max_allowed_packet dépassé) dans la chaîne d'exceptions. */
    private boolean isPacketTooLarge(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("max_allowed_packet") || lower.contains("packet for query is too large")) {
                    return true;
                }
            }
        }
        return false;
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
