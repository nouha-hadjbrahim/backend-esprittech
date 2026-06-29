package tn.esprit.backend_esprittech.mapper.projetcatalogue;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCardResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetCatalogueResponse;
import tn.esprit.backend_esprittech.dto.projetcatalogue.response.ProjetDetailsResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;

import java.util.List;

/**
 * Conversion des entités {@link ProjetCatalogue} vers les différents DTO de réponse.
 * Les méthodes accèdent aux collections paresseuses : elles doivent être appelées dans
 * un contexte transactionnel (ce qui est le cas depuis le service).
 */
@Component
public class ProjetCatalogueMapper {

    /** Carte allégée pour les listes. */
    public ProjetCardResponse toCard(ProjetCatalogue projet) {
        return new ProjetCardResponse(
                projet.getId(),
                projet.getTypeProjet(),
                projet.getTitre(),
                projet.getDescription(),
                projet.getStatut(),
                projet.getScore(),
                idOf(projet.getEncadrant()),
                nomComplet(projet.getEncadrant()),
                idOf(projet.getEquipeRecherche()),
                nomEquipe(projet.getEquipeRecherche()),
                projet.getDateDebut(),
                projet.getDateFin(),
                projet.getDateCreation(),
                domaineNoms(projet),
                technologieNoms(projet)
        );
    }

    /** Représentation canonique complète (résultat des mutations). */
    public ProjetCatalogueResponse toResponse(ProjetCatalogue projet) {
        return new ProjetCatalogueResponse(
                projet.getId(),
                projet.getTypeProjet(),
                projet.getTitre(),
                projet.getDescription(),
                projet.getObjectifs(),
                projet.getDateDebut(),
                projet.getDateFin(),
                projet.getStatut(),
                projet.getScore(),
                idOf(projet.getEncadrant()),
                nomComplet(projet.getEncadrant()),
                idOf(projet.getEquipeRecherche()),
                nomEquipe(projet.getEquipeRecherche()),
                idOf(projet.getChefValidateur()),
                nomComplet(projet.getChefValidateur()),
                projet.getMotifRefus(),
                projet.getDateCreation(),
                projet.getDateValidation(),
                domaineNoms(projet),
                technologieNoms(projet),
                prerequisNoms(projet)
        );
    }

    /** Détails complets pour les pages de détail. */
    public ProjetDetailsResponse toDetails(ProjetCatalogue projet) {
        User encadrant = projet.getEncadrant();
        return new ProjetDetailsResponse(
                projet.getId(),
                projet.getTypeProjet(),
                projet.getTitre(),
                projet.getDescription(),
                projet.getObjectifs(),
                projet.getDateDebut(),
                projet.getDateFin(),
                projet.getStatut(),
                projet.getScore(),
                idOf(encadrant),
                nomComplet(encadrant),
                encadrant != null ? encadrant.getEmail() : null,
                idOf(projet.getEquipeRecherche()),
                nomEquipe(projet.getEquipeRecherche()),
                idOf(projet.getChefValidateur()),
                nomComplet(projet.getChefValidateur()),
                projet.getMotifRefus(),
                projet.getDateCreation(),
                projet.getDateValidation(),
                domaineNoms(projet),
                technologieNoms(projet),
                prerequisNoms(projet)
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long idOf(User user) {
        return user != null ? user.getId() : null;
    }

    private Long idOf(EquipeRecherche equipe) {
        return equipe != null ? equipe.getId() : null;
    }

    private String nomComplet(User user) {
        return user != null ? (user.getPrenom() + " " + user.getNom()) : null;
    }

    private String nomEquipe(EquipeRecherche equipe) {
        return equipe != null ? equipe.getNom() : null;
    }

    private List<String> domaineNoms(ProjetCatalogue projet) {
        return projet.getDomaines().stream().map(SujetDomaine::getNom).sorted().toList();
    }

    private List<String> technologieNoms(ProjetCatalogue projet) {
        return projet.getTechnologies().stream().map(SujetTechnologie::getNom).sorted().toList();
    }

    private List<String> prerequisNoms(ProjetCatalogue projet) {
        return projet.getPrerequis().stream().map(SujetPrerequis::getNom).sorted().toList();
    }
}
