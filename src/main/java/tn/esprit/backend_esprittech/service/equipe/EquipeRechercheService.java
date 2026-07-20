package tn.esprit.backend_esprittech.service.equipe;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.equipe.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.equipe.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.equipe.EquipeResponse;
import tn.esprit.backend_esprittech.entity.equipe.EquipeDomaine;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.AffiliationEnseignant;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.equipe.EquipeMapper;
import tn.esprit.backend_esprittech.repository.equipe.AffiliationEnseignantRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.service.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Gestion des équipes de recherche : création, assignation de chef,
 * consultation, mise à jour, suppression et calcul des membres.
 */
@Service
public class EquipeRechercheService {
    private static final String LIEN_EQUIPES_RECHERCHE = "/frontoffice/equipes-recherche/";


    private final EquipeRechercheRepository equipeRepository;
    private final UserRepository userRepository;
    private final EquipeMapper equipeMapper;
    private final EquipeDomaineService domaineService;
    private final AffiliationEnseignantRepository affiliationRepository;
    private final NotificationService notificationService;

    public EquipeRechercheService(EquipeRechercheRepository equipeRepository,
                                  UserRepository userRepository,
                                  EquipeMapper equipeMapper,
                                  EquipeDomaineService domaineService,
                                  AffiliationEnseignantRepository affiliationRepository,
                                  NotificationService notificationService) {
        this.equipeRepository = equipeRepository;
        this.userRepository = userRepository;
        this.equipeMapper = equipeMapper;
        this.domaineService = domaineService;
        this.affiliationRepository = affiliationRepository;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------------
    // Écriture
    // -------------------------------------------------------------------------

    /**
     * Crée une équipe, promeut le chef désigné au rôle CHEF_EQUIPE
     * et affecte les membres optionnels.
     */
    @Transactional
    @Historise(action = ActionType.CREATE, entityType = EntityType.EQUIPE_RECHERCHE, entityIdExpression = "#result.id()", summaryExpression = "'Création de l''équipe ' + #result.nom()", metadataExpression = "{'nom': #request.nom()}")
    public EquipeResponse create(CreateEquipeRechercheRequest request) {
        if (equipeRepository.existsByNom(request.nom())) {
            throw new BusinessException("Une équipe portant ce nom existe déjà");
        }

        EquipeDomaine domaine = domaineService.getEntityById(request.domaineId());

        User chef = null;
        if (request.chefId() != null) {
            chef = promouvoirChef(request.chefId());
            ensureChefDisponible(chef, null);
        }

        EquipeRecherche equipe = EquipeRecherche.builder()
                .nom(request.nom())
                .description(request.description())
                .domaine(domaine)
                .chef(chef)
                .build();

        assignerMembres(equipe, request.memberIds(), chef);

        return equipeMapper.toResponse(equipeRepository.save(equipe), compterMembres(equipe));
    }

    /** Met à jour une équipe existante (nom, description, chef, membres). */
    @Transactional
    @Historise(action = ActionType.UPDATE, entityType = EntityType.EQUIPE_RECHERCHE, entityIdExpression = "#id", oldValuesExpression = "@equipeRechercheService.getOldValues(#id)", summaryExpression = "'Modification de l''équipe ' + #result.nom()", metadataExpression = "{'nom': #request.nom()}")
    public EquipeResponse update(Long id, UpdateEquipeRechercheRequest request) {
        EquipeRecherche equipe = getEntite(id);

        if (request.nom() != null && equipeRepository.existsByNomAndIdNot(request.nom(), id)) {
            throw new BusinessException("Une équipe portant ce nom existe déjà");
        }

        EquipeDomaine domaine = null;
        if (request.domaineId() != null) {
            domaine = domaineService.getEntityById(request.domaineId());
        }

        User chef = equipe.getChef();
        if (request.chefId() != null) {
            chef = promouvoirChef(request.chefId());
            ensureChefDisponible(chef, id);
        } else if (equipe.getChef() != null) {
            // Chef retiré explicitement
            User oldChef = equipe.getChef();
            oldChef.setRole(Role.ROLE_ENSEIGNANT);
            userRepository.save(oldChef);
            chef = null;
        }

        equipeMapper.updateEntity(request, equipe, domaine);
        equipe.setChef(chef);

        // Le chef ne peut pas être aussi membre
        if (chef != null && equipe.getMembers().contains(chef)) {
            equipe.removeMember(chef);
        }

        if (request.memberIds() != null) {
            viderMembres(equipe);
            assignerMembres(equipe, request.memberIds(), chef);
        }

        return equipeMapper.toResponse(equipeRepository.save(equipe), compterMembres(equipe));
    }

    /** (Ré)assigne uniquement le chef d'une équipe existante. */
    @Transactional
    @Historise(action = ActionType.ASSIGN_CHEF, entityType = EntityType.EQUIPE_RECHERCHE, entityIdExpression = "#equipeId", oldValuesExpression = "@equipeRechercheService.getOldValues(#equipeId)", summaryExpression = "'Assignation du chef à l''équipe ' + #result.nom()", metadataExpression = "{'chefId': #chefId}")
    public EquipeResponse assignerChef(Long equipeId, Long chefId) {
        EquipeRecherche equipe = getEntite(equipeId);
        User chef = promouvoirChef(chefId);
        ensureChefDisponible(chef, equipeId);
        equipe.setChef(chef);
        // Le chef ne peut pas être aussi membre
        if (equipe.getMembers().contains(chef)) {
            equipe.removeMember(chef);
        }
        equipe = equipeRepository.save(equipe);

        notificationService.creerNotification(
                chef.getId(),
                NotificationType.EQUIPE_INVITATION,
                "Chef d'equipe designé",
                "Vous etes maintenant chef de l'equipe \"" + equipe.getNom() + "\".",
                LIEN_EQUIPES_RECHERCHE + equipeId);

        return equipeMapper.toResponse(equipe, compterMembres(equipe));
    }

    /** Ajoute des membres à une équipe existante. */
    @Transactional
    @Historise(action = ActionType.AJOUTER_MEMBRE, entityType = EntityType.EQUIPE_RECHERCHE, entityIdExpression = "#equipeId", oldValuesExpression = "@equipeRechercheService.getOldValues(#equipeId)", summaryExpression = "'Ajout de membres à l''équipe ' + #result.nom()", metadataExpression = "{'memberIds': #memberIds}")
    public EquipeResponse ajouterMembres(Long equipeId, List<Long> memberIds) {
        EquipeRecherche equipe = getEntite(equipeId);
        List<User> oldMembers = new ArrayList<>(equipe.getMembers() != null ? equipe.getMembers() : List.of());
        assignerMembres(equipe, memberIds, equipe.getChef());
        EquipeRecherche saved = equipeRepository.save(equipe);

        for (Long memberId : memberIds) {
            if (oldMembers.stream().noneMatch(u -> u.getId().equals(memberId))) {
                notificationService.creerNotification(
                        memberId,
                        NotificationType.EQUIPE_REJOINTE,
                        "Ajout dans une equipe",
                        "Vous avez ete ajoute a l'equipe \"" + equipe.getNom() + "\".",
                        LIEN_EQUIPES_RECHERCHE + equipeId);
            }
        }

        return equipeMapper.toResponse(saved, compterMembres(equipe));
    }

    /** Retire un membre d'une équipe. */
    @Transactional
    @Historise(action = ActionType.RETIRER_MEMBRE, entityType = EntityType.EQUIPE_RECHERCHE, entityIdExpression = "#equipeId", oldValuesExpression = "@equipeRechercheService.getOldValues(#equipeId)", summaryExpression = "'Retrait d''un membre de l''équipe ' + #result.nom()", metadataExpression = "{'userId': #userId}")
    public EquipeResponse retirerMembre(Long equipeId, Long userId) {
        EquipeRecherche equipe = getEntite(equipeId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable (id=" + userId + ")"));

        if (!equipe.getMembers().contains(user)) {
            throw new BusinessException("Cet utilisateur n'est pas membre de l'équipe");
        }
        if (equipe.getChef() != null && equipe.getChef().getId().equals(userId)) {
            throw new BusinessException("Le chef ne peut pas être retiré des membres. Utilisez l'assignation de chef.");
        }

        equipe.removeMember(user);
        EquipeRecherche saved = equipeRepository.save(equipe);

        List<AffiliationEnseignant> toReset = affiliationRepository
                .findByEnseignantIdAndStatut(userId, StatutAffiliation.ACCEPTEE)
                .stream()
                .filter(a -> a.getEquipe().getId().equals(equipeId))
                .toList();
        for (AffiliationEnseignant a : toReset) {
            a.setStatut(StatutAffiliation.REFUSEE);
            a.setDateDecision(LocalDateTime.now());
            a.setMotifDecision("Retiré de l'équipe");
        }
        affiliationRepository.saveAll(toReset);

        notificationService.creerNotification(
                userId,
                NotificationType.EQUIPE_INVITATION,
                "Retrait de l'equipe",
                "Vous avez ete retire de l'equipe \"" + equipe.getNom() + "\".",
                LIEN_EQUIPES_RECHERCHE + equipeId);

        return equipeMapper.toResponse(saved, compterMembres(equipe));
    }

    /** Retire le chef d'une équipe et le rétrograde au rôle ENSEIGNANT. */
    @Transactional
    @Historise(action = ActionType.RETIRER_CHEF, entityType = EntityType.EQUIPE_RECHERCHE, entityIdExpression = "#equipeId", oldValuesExpression = "@equipeRechercheService.getOldValues(#equipeId)", summaryExpression = "'Retrait du chef de l''équipe ' + #result.nom()")
    public EquipeResponse retirerChef(Long equipeId) {
        EquipeRecherche equipe = getEntite(equipeId);
        User chef = equipe.getChef();
        equipe.setChef(null);
        EquipeRecherche saved = equipeRepository.save(equipe);
        if (chef != null) {
            chef.setRole(Role.ROLE_ENSEIGNANT);
            userRepository.save(chef);

            notificationService.creerNotification(
                    chef.getId(),
                    NotificationType.EQUIPE_INVITATION,
                    "Retrait du role de chef",
                    "Vous n'etes plus chef de l'equipe \"" + equipe.getNom() + "\".",
                    LIEN_EQUIPES_RECHERCHE + equipeId);
        }
        return equipeMapper.toResponse(saved, compterMembres(equipe));
    }

    /** Supprime une équipe (uniquement si elle ne contient aucun membre). */
    @Transactional
    @Historise(action = ActionType.DELETE, entityType = EntityType.EQUIPE_RECHERCHE, entityIdExpression = "#id", oldValuesExpression = "@equipeRechercheService.getOldValues(#id)", summaryExpression = "'Suppression de l''équipe ' + @equipeRechercheService.getNomById(#id)")
    public void delete(Long id) {
        EquipeRecherche equipe = getEntite(id);
        if (compterMembres(equipe) > 0) {
            throw new BusinessException("Impossible de supprimer une équipe qui contient encore des membres.");
        }
        equipeRepository.delete(equipe);
    }

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EquipeResponse> listerToutes() {
        return equipeRepository.findAll().stream()
                .map(e -> equipeMapper.toResponse(e, compterMembres(e)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipeResponse findById(Long id) {
        EquipeRecherche equipe = getEntite(id);
        return equipeMapper.toResponse(equipe, compterMembres(equipe));
    }

    private EquipeRecherche getEntite(Long equipeId) {
        Long tid = Objects.requireNonNull(equipeId, "equipeId");
        return equipeRepository.findById(tid)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable (id=" + equipeId + ")"));
    }

    public String getNomById(Long id) {
        return equipeRepository.findById(id)
                .map(EquipeRecherche::getNom)
                .orElse("Inconnu");
    }

    public Map<String, Object> getOldValues(Long id) {
        EquipeRecherche e = getEntite(id);
        Map<String, Object> map = new HashMap<>();
        map.put("nom", e.getNom());
        map.put("description", e.getDescription());
        map.put("domaineId", e.getDomaine() != null ? e.getDomaine().getId() : null);
        map.put("chefId", e.getChef() != null ? e.getChef().getId() : null);
        map.put("memberIds", e.getMembers() != null
                ? e.getMembers().stream().map(User::getId).toList()
                : List.of());
        return map;
    }

    /**
     * Membres d'une équipe : le chef + les enseignants affiliés.
     * Le chef est toujours placé en tête de liste.
     */
    @Transactional(readOnly = true)
    public List<User> getMembres(Long equipeId) {
        EquipeRecherche equipe = getEntite(equipeId);
        List<User> membres = new ArrayList<>();
        if (equipe.getChef() != null) {
            membres.add(equipe.getChef());
        }
        membres.addAll(equipe.getMembers());
        return membres;
    }

    /** Nombre total de membres (chef + membres) utilisé pour l'exposition API. */
    private int compterMembres(EquipeRecherche equipe) {
        int count = equipe.getChef() != null ? 1 : 0;
        count += equipe.getMembers() != null ? equipe.getMembers().size() : 0;
        return count;
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    /**
     * Vérifie l'éligibilité du futur chef (rôle ENSEIGNANT ou CHEF_EQUIPE)
     * puis le promeut au rôle CHEF_EQUIPE.
     */
    private User promouvoirChef(Long chefId) {
        User chef = userRepository.findById(Objects.requireNonNull(chefId, "chefId"))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable (id=" + chefId + ")"));

        if (chef.getRole() != Role.ROLE_ENSEIGNANT && chef.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new BusinessException("Le chef d'équipe doit avoir le rôle ENSEIGNANT");
        }

        chef.setRole(Role.ROLE_CHEF_EQUIPE);
        return userRepository.save(chef);
    }

    /** S'assure qu'un chef n'est pas déjà responsable ou membre d'une autre équipe. */
    private void ensureChefDisponible(User chef, Long currentEquipeId) {
        Long safeId = currentEquipeId == null ? -1L : currentEquipeId;
        if (equipeRepository.existsByChefIdAndIdNot(chef.getId(), safeId)) {
            throw new BusinessException("Cet enseignant est déjà chef d'une autre équipe");
        }
        if (chef.getEquipeRecherche() != null
                && (currentEquipeId == null || !currentEquipeId.equals(chef.getEquipeRecherche().getId()))) {
            throw new BusinessException("Cet enseignant appartient déjà à une autre équipe");
        }
    }

    /** Affecte une liste de membres à l'équipe en validant chaque utilisateur. */
    private void assignerMembres(EquipeRecherche equipe, List<Long> memberIds, User chef) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(memberIds);
        if (chef != null && uniqueIds.contains(chef.getId())) {
            throw new BusinessException("Le chef ne peut pas figurer dans la liste des membres");
        }

        List<User> membres = new ArrayList<>(userRepository.findAllByIdIn(new ArrayList<>(uniqueIds)));
        if (membres.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs membres sont introuvables");
        }

        for (User user : membres) {
            if (user.getRole() != Role.ROLE_ENSEIGNANT) {
                throw new BusinessException("Les membres doivent avoir le rôle ENSEIGNANT");
            }
            if (user.getEquipeRecherche() != null && !user.getEquipeRecherche().equals(equipe)) {
                throw new BusinessException("Un ou plusieurs membres appartiennent déjà à une autre équipe");
            }
        }

        for (User user : membres) {
            equipe.addMember(user);
            traiterDemandesEnAttente(user, equipe);
        }
    }

    private void traiterDemandesEnAttente(User user, EquipeRecherche equipe) {
        List<AffiliationEnseignant> pending = affiliationRepository
                .findByEnseignantIdAndStatut(user.getId(), StatutAffiliation.EN_ATTENTE);
        if (pending.isEmpty()) return;

        for (AffiliationEnseignant req : pending) {
            if (req.getEquipe().getId().equals(equipe.getId())) {
                req.setStatut(StatutAffiliation.ACCEPTEE);
            } else {
                req.setStatut(StatutAffiliation.REFUSEE);
                req.setMotifDecision("Déjà membre d'une équipe");
            }
            req.setDateDecision(LocalDateTime.now());
        }
        affiliationRepository.saveAll(pending);
    }

    /** Dissocie tous les membres actuels de l'équipe. */
    private void viderMembres(EquipeRecherche equipe) {
        List<User> existants = new ArrayList<>(
                Objects.requireNonNull(equipe, "equipe").getMembers());
        for (User user : existants) {
            equipe.removeMember(user);
        }
    }
}
