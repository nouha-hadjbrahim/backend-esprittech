package tn.esprit.backend_esprittech.service.livrable;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LivrableService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "png", "jpg", "jpeg", "txt", "md", "markdown", "html", "htm", "json", "xml", "lcov"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg",
            "text/plain",
            "text/markdown",
            "text/html",
            "application/json",
            "application/xml",
            "text/xml",
            "application/octet-stream"
    );

    private static final Set<StatutSujet> DEPOT_ALLOWED_STATUSES = Set.of(
            StatutSujet.REALISATION_EN_COURS,
            StatutSujet.REALISATION_TERMINEE
    );

    private final LivrableRepository livrableRepository;
    private final SujetProjetRepository sujetProjetRepository;
    private final UserService userService;
    private final MinioStorageService storageService;

    @Historise(action = ActionType.UPLOAD, entityType = EntityType.LIVRABLE, entityIdExpression = "#result.id()", summaryExpression = "'Upload livrable ' + #result.nom()")
    public LivrableResponse upload(Long projetId,
                                   TypeLivrable typeLivrable,
                                   String nom,
                                   String description,
                                   MultipartFile file) {
        User current = userService.getCurrentUser();
        SujetProjet projet = getProjet(projetId);
        ensureCanDeposit(current, projet);
        validateFile(file);
        validateText(nom, "Le nom du livrable est obligatoire.");

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String objectName = buildObjectName(projetId, originalFileName);
        MinioStorageService.StoredObject storedObject = storageService.upload(objectName, file);

        Livrable livrable = new Livrable();
        livrable.setSujetProjet(projet);
        livrable.setTypeLivrable(typeLivrable);
        livrable.setNom(nom.trim());
        livrable.setDescription(trimToNull(description));
        livrable.setOriginalFileName(originalFileName);
        livrable.setObjectName(storedObject.objectName());
        livrable.setContentType(storedObject.contentType());
        livrable.setSize(storedObject.size());
        livrable.setDeposant(current);

        return toResponse(livrableRepository.save(livrable));
    }

    @Historise(action = ActionType.LINK, entityType = EntityType.LIVRABLE, entityIdExpression = "#result.id()", summaryExpression = "'Ajout lien livrable ' + #result.nom()")
    public LivrableResponse addLink(Long projetId, LivrableLinkRequest request) {
        User current = userService.getCurrentUser();
        SujetProjet projet = getProjet(projetId);
        ensureCanDeposit(current, projet);
        validateText(request.nom(), "Le nom du livrable est obligatoire.");
        validateUrl(request.lienExterne());

        Livrable livrable = new Livrable();
        livrable.setSujetProjet(projet);
        livrable.setTypeLivrable(request.typeLivrable());
        livrable.setNom(request.nom().trim());
        livrable.setDescription(trimToNull(request.description()));
        livrable.setLienExterne(request.lienExterne().trim());
        livrable.setDeposant(current);

        return toResponse(livrableRepository.save(livrable));
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> findByProjet(Long projetId) {
        User current = userService.getCurrentUser();
        SujetProjet projet = getProjet(projetId);
        ensureCanRead(current, projet);
        return livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(projetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> findAllActiveForAdmin() {
        ensureAdmin(userService.getCurrentUser());
        return livrableRepository.findByActifTrueOrderByDateDepotDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LivrableResponse findById(Long id) {
        Livrable livrable = getLivrable(id);
        ensureCanRead(userService.getCurrentUser(), livrable.getSujetProjet());
        return toResponse(livrable);
    }

    public Map<String, Object> getOldValues(Long id) {
        Livrable l = getLivrable(id);
        Map<String, Object> map = new HashMap<>();
        map.put("nom", l.getNom());
        map.put("typeLivrable", l.getTypeLivrable());
        map.put("actif", l.getActif());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.LIVRABLE, entityIdExpression = "#id", oldValuesExpression = "@livrableService.getOldValues(#id)", summaryExpression = "'Modification livrable ' + #result.nom()")
    public LivrableResponse update(Long id, LivrableUpdateRequest request) {
        Livrable livrable = getLivrable(id);
        ensureCanManage(userService.getCurrentUser(), livrable.getSujetProjet());
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

    @Historise(action = ActionType.DELETE, entityType = EntityType.LIVRABLE, entityIdExpression = "#id", oldValuesExpression = "@livrableService.getOldValues(#id)", newValuesExpression = "{'actif': false}", summaryExpression = "'Suppression livrable (id=' + #id + ')'")
    public void delete(Long id) {
        Livrable livrable = getLivrable(id);
        ensureCanManage(userService.getCurrentUser(), livrable.getSujetProjet());
        livrable.setActif(false);
        livrableRepository.save(livrable);
    }

    @Transactional(readOnly = true)
    public LivrableDownload download(Long id) {
        Livrable livrable = getLivrable(id);
        ensureCanRead(userService.getCurrentUser(), livrable.getSujetProjet());
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

    public boolean hasActiveLivrables(Long projetId) {
        return livrableRepository.existsBySujetProjetIdAndActifTrue(projetId);
    }

    private SujetProjet getProjet(Long projetId) {
        return sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));
    }

    private Livrable getLivrable(Long id) {
        return livrableRepository.findById(id)
                .filter(livrable -> Boolean.TRUE.equals(livrable.getActif()))
                .orElseThrow(() -> new ResourceNotFoundException("Livrable introuvable avec id: " + id));
    }

    private void ensureCanDeposit(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (current.getRole() != Role.ROLE_ENSEIGNANT || !projet.getEncadrantId().equals(current.getId())) {
            throw new AccessDeniedException("Acces refuse au depot de livrables");
        }
        if (!DEPOT_ALLOWED_STATUSES.contains(projet.getStatut())) {
            throw new BusinessException("Les livrables peuvent etre deposes pendant la realisation ou apres terminaison.");
        }
    }

    private void ensureCanRead(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN || current.getRole() == Role.ROLE_CI) {
            return;
        }
        if (projet.getEncadrantId().equals(current.getId())) {
            return;
        }
        throw new AccessDeniedException("Acces refuse au livrable");
    }

    private void ensureCanManage(User current, SujetProjet projet) {
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (current.getRole() == Role.ROLE_ENSEIGNANT && projet.getEncadrantId().equals(current.getId())) {
            return;
        }
        throw new AccessDeniedException("Acces refuse a la gestion du livrable");
    }

    private void ensureAdmin(User current) {
        if (current.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Acces admin requis");
        }
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
            throw new BusinessException("Format non autorise. Formats acceptes: pdf, doc, docx, png, jpg, jpeg, txt, md, html, json, xml, lcov.");
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
        return "livrables/" + projetId + "/" + UUID.randomUUID() + "-" + sanitizeFileName(originalFileName);
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

    private LivrableResponse toResponse(Livrable livrable) {
        User deposant = livrable.getDeposant();
        return new LivrableResponse(
                livrable.getId(),
                livrable.getSujetProjet().getId(),
                livrable.getSujetProjet().getTitre(),
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
