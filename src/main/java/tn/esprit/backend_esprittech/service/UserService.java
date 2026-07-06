package tn.esprit.backend_esprittech.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.request.CreateUserRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateUserRequest;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailAlreadyExistsException;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EquipeRechercheRepository equipeRechercheRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EquipeRechercheRepository equipeRechercheRepository,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.equipeRechercheRepository = equipeRechercheRepository;
        this.userMapper = userMapper;
    }

    /** Création directe d'un compte par l'admin (sans validation contre le référentiel). */
    @Transactional
    @Historise(action = ActionType.CREATE, entityType = EntityType.USER, entityIdExpression = "#result.id()", summaryExpression = "'Création utilisateur ' + #result.getEmail()")
    public User create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Un compte existe déjà pour cet email");
        }
        if (userRepository.existsByIdentifiant(request.identifiant())) {
            throw new EmailAlreadyExistsException("Un compte existe déjà pour cet identifiant");
        }

        // ROLE_ETUDIANT -> ETUDIANT ; tous les autres rôles sont des profils enseignant
        TypeUtilisateur type = (request.role() == Role.ROLE_ETUDIANT)
                ? TypeUtilisateur.ETUDIANT : TypeUtilisateur.ENSEIGNANT;

        User user = User.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .identifiant(request.identifiant())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .typeUtilisateur(type)
                .enabled(request.enabled())
                .build();
        return userRepository.save(user);
    }

    /** Liste paginée des utilisateurs, filtrée par recherche libre si fournie. */
    public Page<User> list(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return userRepository.search(search.trim(), pageable);
        }
        return userRepository.findAll(pageable);
    }

    /**
     * Liste paginée des utilisateurs éligibles pour être chef ou membre
     * d'une équipe donnée.
     * @param type "CHEF" ou "MEMBER"
     */
    public Page<User> listEligible(String search, Long equipeId, String type, Pageable pageable) {
        List<Role> roles = List.of(Role.ROLE_ENSEIGNANT);
        String q = search != null ? search.trim() : "";
        if ("CHEF".equalsIgnoreCase(type) && equipeId != null) {
            return userRepository.searchEligible(q, equipeId, roles, pageable);
        }
        return userRepository.searchEligibleMembers(q, roles, pageable);
    }

    public Map<String, Object> getUserOldValues(Long id) {
        User u = findById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("nom", u.getNom());
        map.put("prenom", u.getPrenom());
        map.put("email", u.getEmail());
        map.put("role", u.getRole());
        return map;
    }

    /** Met à jour l'identité, le rôle et l'état d'activation d'un utilisateur. */
    @Transactional
    @Historise(action = ActionType.UPDATE, entityType = EntityType.USER, entityIdExpression = "#id", oldValuesExpression = "@userService.getUserOldValues(#id)", summaryExpression = "'Modification utilisateur ' + #result.getEmail()")
    public User update(Long id, UpdateUserRequest request) {
        User user = findById(id);

        // L'email doit rester unique s'il change
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Un compte existe déjà pour cet email");
        }

        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        return userRepository.save(user);
    }

    /** Supprime un utilisateur ; un admin ne peut pas supprimer son propre compte. */
    @Transactional
    @Historise(action = ActionType.DELETE, entityType = EntityType.USER, entityIdExpression = "#id", oldValuesExpression = "@userService.getUserOldValues(#id)", summaryExpression = "'Suppression utilisateur (id=' + #id + ')'")
    public void delete(Long id) {
        User user = findById(id);
        User current = getCurrentUser();
        if (current.getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas supprimer votre propre compte");
        }
        userRepository.delete(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable (id=" + id + ")"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable (" + email + ")"));
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() instanceof String) {
            throw new NoSuchElementException("Aucun utilisateur authentifié");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return findByEmail(authentication.getName());
    }

    /** Profil enrichi de l'utilisateur courant (avec son affiliation), pour /auth/me. */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserResponse() {
        return toEnrichedResponse(getCurrentUser());
    }

    /**
     * Construit un {@link UserResponse} enrichi de l'affiliation à une équipe de recherche.
     * L'enseignant membre est affilié via {@code user.equipeRecherche} ; le chef d'équipe
     * l'est via l'équipe dont il est responsable. Exécutée dans une transaction pour
     * initialiser les associations paresseuses.
     */
    @Transactional(readOnly = true)
    public UserResponse toEnrichedResponse(User user) {
        User managed = userRepository.findById(user.getId()).orElse(user);
        EquipeRecherche equipe = managed.getEquipeRecherche();
        if (equipe == null) {
            equipe = equipeRechercheRepository.findByChefId(managed.getId()).orElse(null);
        }
        Long equipeId = (equipe != null) ? equipe.getId() : null;
        String equipeNom = (equipe != null) ? equipe.getNom() : null;
        return userMapper.toResponse(managed, equipeId != null, equipeId, equipeNom);
    }
}
