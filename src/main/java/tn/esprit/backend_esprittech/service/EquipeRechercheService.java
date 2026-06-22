package tn.esprit.backend_esprittech.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRequest;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestion des équipes de recherche : création, assignation de chef,
 * consultation et calcul des membres.
 */
@Service
public class EquipeRechercheService {

    private final EquipeRechercheRepository equipeRepository;
    private final UserRepository userRepository;

    public EquipeRechercheService(EquipeRechercheRepository equipeRepository,
                                  UserRepository userRepository) {
        this.equipeRepository = equipeRepository;
        this.userRepository = userRepository;
    }

    /** Crée une équipe et promeut le chef désigné au rôle CHEF_EQUIPE. */
    @Transactional
    public EquipeRecherche creerEquipe(CreateEquipeRequest request) {
        if (equipeRepository.existsByNom(request.nom())) {
            throw new IllegalArgumentException("Une équipe portant ce nom existe déjà");
        }

        User chef = promouvoirChef(request.chefId());

        EquipeRecherche equipe = EquipeRecherche.builder()
                .nom(request.nom())
                .description(request.description())
                .chef(chef)
                .build();

        return equipeRepository.save(equipe);
    }

    /** (Ré)assigne le chef d'une équipe existante et le promeut au rôle CHEF_EQUIPE. */
    @Transactional
    public EquipeRecherche assignerChef(Long equipeId, Long chefId) {
        EquipeRecherche equipe = getEntite(equipeId);
        User chef = promouvoirChef(chefId);
        equipe.setChef(chef);
        return equipeRepository.save(equipe);
    }

    public List<EquipeRecherche> listerToutes() {
        return equipeRepository.findAll();
    }

    public EquipeRecherche getEntite(Long equipeId) {
        return equipeRepository.findById(equipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable (id=" + equipeId + ")"));
    }

    /** Membres d'une équipe : le chef désigné par l'admin. */
    public List<User> getMembres(Long equipeId) {
        EquipeRecherche equipe = getEntite(equipeId);
        List<User> membres = new ArrayList<>();
        if (equipe.getChef() != null) {
            membres.add(equipe.getChef());
        }
        return membres;
    }

    /** Nombre de membres (le chef) utilisé pour l'exposition API. */
    public int compterMembres(EquipeRecherche equipe) {
        return equipe.getChef() != null ? 1 : 0;
    }

    /** Vérifie l'éligibilité du futur chef (enseignant existant) puis le promeut. */
    private User promouvoirChef(Long chefId) {
        User chef = userRepository.findById(chefId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable (id=" + chefId + ")"));

        if (chef.getRole() != Role.ROLE_ENSEIGNANT && chef.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new IllegalArgumentException(
                    "Le chef d'équipe doit être un utilisateur de rôle ENSEIGNANT");
        }

        chef.setRole(Role.ROLE_CHEF_EQUIPE);
        return userRepository.save(chef);
    }
}
