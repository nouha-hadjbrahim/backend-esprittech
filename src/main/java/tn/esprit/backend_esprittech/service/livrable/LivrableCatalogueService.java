package tn.esprit.backend_esprittech.service.livrable;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.dto.livrable.LivrableCatalogueResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.notification.NotificationService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Gestion des livrables rattachés aux projets du catalogue applicatif.
 *
 * <p>Parallèle à {@code LivrableService} (rattaché aux sujets) mais dédié à
 * {@link ProjetCatalogue} afin de ne pas interférer avec la fonctionnalité des sujets.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LivrableCatalogueService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "png", "jpg", "jpeg", "txt"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg",
            "text/plain"
    );

    private final LivrableCatalogueRepository livrableRepository;
    private final ProjetCatalogueRepository projetRepository;
    private final UserService userService;
    private final MinioStorageService storageService;
    private final NotificationService notificationService;

    public LivrableCatalogueResponse upload(Long projetId,
                                            TypeLivrable typeLivrable,
                                            String nom,
                                            String description,
                                            MultipartFile file) {
        User current = userService.getCurrentUser();
        ProjetCatalogue projet = getProjet(projetId);
        ensureCanDeposit(current, projet);
        validateFile(file);
        validateText(nom, "Le nom du livrable est obligatoire.");

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String objectName = buildObjectName(projetId, originalFileName);
        MinioStorageService.StoredObject storedObject = storageService.upload(objectName, file);

        LivrableCatalogue livrable = new LivrableCatalogue();
        livrable.setProjet(projet);
        livrable.setTypeLivrable(typeLivrable);
        livrable.setNom(nom.trim());
        livrable.setDescription(trimToNull(description));
        livrable.setOriginalFileName(originalFileName);
        livrable.setObjectName(storedObject.objectName());
        livrable.setContentType(storedObject.contentType());
        livrable.setSize(storedObject.size());
        livrable.setDeposant(current);

        LivrableCatalogueResponse response = toResponse(livrableRepository.save(livrable));

        if (projet.getEncadrant() != null) {
            notificationService.creerNotification(
                    projet.getEncadrant().getId(),
                    NotificationType.LIVRABLE_TELECHARGE,
                    "Livrable telecharge",
                    "Un nouveau livrable \"" + nom + "\" a ete depose sur le projet catalogue \"" + projet.getTitre() + "\".",
                    "/frontoffice/mes-projets");
        }

        return response;
    }

    public LivrableCatalogueResponse addLink(Long projetId, LivrableLinkRequest request) {
        User current = userService.getCurrentUser();
        ProjetCatalogue projet = getProjet(projetId);
        ensureCanDeposit(current, projet);
        validateText(request.nom(), "Le nom du livrable est obligatoire.");
        validateUrl(request.lienExterne());

        LivrableCatalogue livrable = new LivrableCatalogue();
        livrable.setProjet(projet);
        livrable.setTypeLivrable(request.typeLivrable());
        livrable.setNom(request.nom().trim());
        livrable.setDescription(trimToNull(request.description()));
        livrable.setLienExterne(request.lienExterne().trim());
        livrable.setDeposant(current);

        LivrableCatalogueResponse response = toResponse(livrableRepository.save(livrable));

        if (projet.getEncadrant() != null) {
            notificationService.creerNotification(
                    projet.getEncadrant().getId(),
                    NotificationType.LIVRABLE_TELECHARGE,
                    "Lien livrable ajoute",
                    "Un nouveau lien livrable \"" + request.nom() + "\" a ete ajoute sur le projet catalogue \"" + projet.getTitre() + "\".",
                    "/frontoffice/mes-projets");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<LivrableCatalogueResponse> findByProjet(Long projetId) {
        User current = userService.getCurrentUser();
        ProjetCatalogue projet = getProjet(projetId);
        ensureCanRead(current, projet);
        return livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(projetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LivrableCatalogueResponse findById(Long id) {
        LivrableCatalogue livrable = getLivrable(id);
        ensureCanRead(userService.getCurrentUser(), livrable.getProjet());
        return toResponse(livrable);
    }

    public LivrableCatalogueResponse update(Long id, LivrableUpdateRequest request) {
        LivrableCatalogue livrable = getLivrable(id);
        ensureCanManage(userService.getCurrentUser(), livrable.getProjet());
        validateText(request.nom(), "Le nom du livrable est obligatoire.");
        if (StringUtils.hasText(request.lienExterne())) {
            validateUrl(request.lienExterne());
        }

        livrable.setTypeLivrable(request.typeLivrable());
        livrable.setNom(request.nom().trim());
        livrable.setDescription(trimToNull(request.description()));
        livrable.setLienExterne(trimToNull(request.lienExterne()));
        if (request.actif() != null) {
            livrable.setActif(request.actif());
        }
        return toResponse(livrableRepository.save(livrable));
    }

    public void delete(Long id) {
        LivrableCatalogue livrable = getLivrable(id);
        ensureCanManage(userService.getCurrentUser(), livrable.getProjet());
        livrable.setActif(false);
        livrableRepository.save(livrable);
    }

    @Transactional(readOnly = true)
    public LivrableDownload download(Long id) {
        LivrableCatalogue livrable = getLivrable(id);
        ensureCanRead(userService.getCurrentUser(), livrable.getProjet());
        if (!StringUtils.hasText(livrable.getObjectName())) {
            throw new BusinessException("Ce livrable est un lien externe et ne contient pas de fichier.");
        }
        MinioStorageService.DownloadedObject file = storageService.download(livrable.getObjectName());
        return new LivrableDownload(
                file.inputStream(),
                StringUtils.hasText(livrable.getOriginalFileName()) ? livrable.getOriginalFileName() : livrable.getNom(),
                file.contentType(),
                file.size()
        );
    }

    private ProjetCatalogue getProjet(Long projetId) {
        return projetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));
    }

    private LivrableCatalogue getLivrable(Long id) {
        return livrableRepository.findById(id)
                .filter(livrable -> Boolean.TRUE.equals(livrable.getActif()))
                .orElseThrow(() -> new ResourceNotFoundException("Livrable introuvable avec id: " + id));
    }

    private void ensureCanDeposit(User current, ProjetCatalogue projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (current.getRole() != Role.ROLE_ENSEIGNANT || !isEncadrant(current, projet)) {
            throw new AccessDeniedException("Acces refuse au depot de livrables");
        }
        if (!StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())) {
            throw new BusinessException("Les livrables peuvent etre deposes une fois le projet valide.");
        }
    }

    private void ensureCanRead(User current, ProjetCatalogue projet) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        if (isEncadrant(current, projet) || isChefDeLEquipe(current, projet)) {
            return;
        }
        // Projet publié au catalogue : ses livrables sont visibles par l'audience du catalogue
        // (enseignants et chefs d'équipe), en cohérence avec la visibilité du projet lui-même.
        if (StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())
                && (current.getRole() == Role.ROLE_ENSEIGNANT || current.getRole() == Role.ROLE_CHEF_EQUIPE)) {
            return;
        }
        throw new AccessDeniedException("Acces refuse au livrable");
    }

    private void ensureCanManage(User current, ProjetCatalogue projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (current.getRole() == Role.ROLE_ENSEIGNANT && isEncadrant(current, projet)) {
            return;
        }
        throw new AccessDeniedException("Acces refuse a la gestion du livrable");
    }

    private boolean isEncadrant(User current, ProjetCatalogue projet) {
        return projet.getEncadrant() != null && projet.getEncadrant().getId().equals(current.getId());
    }

    private boolean isChefDeLEquipe(User current, ProjetCatalogue projet) {
        EquipeRecherche equipe = projet.getEquipeRecherche();
        return equipe != null && equipe.getChef() != null && equipe.getChef().getId().equals(current.getId());
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier est obligatoire.");
        }
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (!StringUtils.hasText(originalFileName)
                || originalFileName.contains("..")
                || originalFileName.contains("/")
                || originalFileName.contains("\\")) {
            throw new BusinessException("Nom de fichier invalide.");
        }
        String extension = extensionOf(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Format non autorise. Formats acceptes: pdf, doc, docx, png, jpg, jpeg, txt.");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Type MIME non autorise.");
        }
    }

    private void validateText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
    }

    private void validateUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            throw new BusinessException("Le lien externe doit etre une URL http(s) valide.");
        }
    }

    private String buildObjectName(Long projetId, String originalFileName) {
        return "livrables-catalogue/" + projetId + "/" + UUID.randomUUID() + "-" + sanitizeFileName(originalFileName);
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String extensionOf(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LivrableCatalogueResponse toResponse(LivrableCatalogue livrable) {
        User deposant = livrable.getDeposant();
        return new LivrableCatalogueResponse(
                livrable.getId(),
                livrable.getProjet().getId(),
                livrable.getProjet().getTitre(),
                livrable.getTypeLivrable(),
                livrable.getNom(),
                livrable.getDescription(),
                livrable.getOriginalFileName(),
                livrable.getObjectName(),
                livrable.getContentType(),
                livrable.getSize(),
                livrable.getLienExterne(),
                deposant.getId(),
                deposant.getPrenom() + " " + deposant.getNom(),
                livrable.getDateDepot(),
                livrable.getActif()
        );
    }

    public record LivrableDownload(InputStream inputStream, String filename, String contentType, long size) {
    }
}
