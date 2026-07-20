package tn.esprit.backend_esprittech.service.livrable;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.dto.livrable.CataloguePublicLivrableResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableCatalogueResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.service.UserService;
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
    private final LivrableRepository sujetLivrableRepository;
    private final ProjetCatalogueRepository projetRepository;
    private final UserService userService;
    private final MinioStorageService storageService;

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
        // Projet publié : livrables visibles par tout utilisateur authentifié (détail catalogue).
        if (StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())) {
            return;
        }
        throw new AccessDeniedException("Acces refuse au livrable");
    }

    /**
     * Livrables d'un projet publié au catalogue : dépôt catalogue s'il existe,
     * sinon livrables du sujet d'origine (après terminaison / publication automatique).
     */
    @Transactional(readOnly = true)
    public List<CataloguePublicLivrableResponse> findPublishedForPublicCatalogue(Long projetId) {
        ProjetCatalogue projet = getProjet(projetId);
        if (!StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())) {
            throw new ResourceNotFoundException("Projet introuvable dans le catalogue (id=" + projetId + ")");
        }

        List<LivrableCatalogue> catalogueLivrables =
                livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(projetId);
        if (!catalogueLivrables.isEmpty()) {
            return catalogueLivrables.stream()
                    .map(livrable -> toPublicResponse(livrable, false))
                    .toList();
        }

        Long sujetId = projet.getSujetId();
        if (sujetId == null) {
            return List.of();
        }

        return sujetLivrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(sujetId)
                .stream()
                .map(livrable -> toPublicResponseFromSujet(livrable, projet))
                .toList();
    }

    @Transactional(readOnly = true)
    public LivrableDownload downloadPublished(Long projetId, Long livrableId, boolean fromSujet) {
        ProjetCatalogue projet = getProjet(projetId);
        if (!StatutProjet.STATUTS_CATALOGUE.contains(projet.getStatut())) {
            throw new ResourceNotFoundException("Projet introuvable dans le catalogue (id=" + projetId + ")");
        }

        if (fromSujet) {
            if (projet.getSujetId() == null) {
                throw new ResourceNotFoundException("Livrable introuvable avec id: " + livrableId);
            }
            Livrable livrable = sujetLivrableRepository.findById(livrableId)
                    .filter(item -> Boolean.TRUE.equals(item.getActif()))
                    .filter(item -> item.getSujetProjet() != null
                            && projet.getSujetId().equals(item.getSujetProjet().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Livrable introuvable avec id: " + livrableId));
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

        LivrableCatalogue livrable = getLivrable(livrableId);
        if (!projet.getId().equals(livrable.getProjet().getId())) {
            throw new ResourceNotFoundException("Livrable introuvable avec id: " + livrableId);
        }
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

    private CataloguePublicLivrableResponse toPublicResponse(LivrableCatalogue livrable, boolean fromSujet) {
        User deposant = livrable.getDeposant();
        return new CataloguePublicLivrableResponse(
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
                livrable.getActif(),
                fromSujet
        );
    }

    private CataloguePublicLivrableResponse toPublicResponseFromSujet(Livrable livrable, ProjetCatalogue projet) {
        User deposant = livrable.getDeposant();
        return new CataloguePublicLivrableResponse(
                livrable.getId(),
                projet.getId(),
                projet.getTitre(),
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
                livrable.getActif(),
                true
        );
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
