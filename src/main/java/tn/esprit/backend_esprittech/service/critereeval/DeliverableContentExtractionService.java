package tn.esprit.backend_esprittech.service.critereeval;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.dto.critereeval.MlDeliverableContent;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class DeliverableContentExtractionService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "docx", "txt", "md", "markdown", "html", "htm",
            "png", "jpg", "jpeg", "json", "xml", "lcov"
    );

    private final MinioStorageService storageService;

    @Value("${evaluation.limits.max-file-bytes:5242880}")
    private long maxFileBytes;

    /**
     * Vue commune sur un livrable, indépendante de l'entité source ({@link Livrable} rattaché à un
     * sujet, ou {@link LivrableCatalogue} rattaché à un projet catalogue), pour partager la logique
     * d'extraction de contenu vers le moteur ML.
     */
    private record DeliverableRef(
            Long id,
            String objectName,
            Long size,
            String typeLivrableName,
            String contentType,
            String nom,
            String originalFileName
    ) {
        static DeliverableRef of(Livrable livrable) {
            return new DeliverableRef(
                    livrable.getId(),
                    livrable.getObjectName(),
                    livrable.getSize(),
                    livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name(),
                    livrable.getContentType(),
                    livrable.getNom(),
                    livrable.getOriginalFileName()
            );
        }

        static DeliverableRef of(LivrableCatalogue livrable) {
            return new DeliverableRef(
                    livrable.getId(),
                    livrable.getObjectName(),
                    livrable.getSize(),
                    livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name(),
                    livrable.getContentType(),
                    livrable.getNom(),
                    livrable.getOriginalFileName()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<MlDeliverableContent> extract(List<Livrable> livrables) {
        return livrables.stream()
                .map(DeliverableRef::of)
                .filter(ref -> StringUtils.hasText(ref.objectName()))
                .map(this::prepare)
                .toList();
    }

    /** Variante pour les livrables du catalogue applicatif ({@link LivrableCatalogue}). */
    @Transactional(readOnly = true)
    public List<MlDeliverableContent> extractCatalogue(List<LivrableCatalogue> livrables) {
        return livrables.stream()
                .map(DeliverableRef::of)
                .filter(ref -> StringUtils.hasText(ref.objectName()))
                .map(this::prepare)
                .toList();
    }

    private MlDeliverableContent prepare(DeliverableRef livrable) {
        if (!StringUtils.hasText(livrable.objectName())) {
            return unsupported(livrable, "EXTERNAL_LINK_NOT_A_MINIO_OBJECT");
        }
        if (!validObjectName(livrable.objectName())) {
            return securityRejected(livrable, "INVALID_MINIO_OBJECT_NAME");
        }
        if (livrable.size() != null && livrable.size() > maxFileBytes) {
            return securityRejected(livrable, "FILE_SIZE_LIMIT_EXCEEDED");
        }
        String extension = extensionOf(livrable);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            return unsupported(livrable, "UNSUPPORTED_EXTENSION");
        }

        try {
            MinioStorageService.DownloadedObject downloaded = storageService.download(livrable.objectName());
            if (downloaded.size() > maxFileBytes) {
                return securityRejected(livrable, "MINIO_OBJECT_SIZE_LIMIT_EXCEEDED");
            }
            try (InputStream inputStream = downloaded.inputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                String hash = sha256(bytes);
                String base64 = Base64.getEncoder().encodeToString(bytes);
                String sourceType = sourceType(livrable, extension);
                String content = inlineTextIfSafe(sourceType, bytes);
                return new MlDeliverableContent(
                        livrable.id(),
                        livrable.typeLivrableName(),
                        sourceType,
                        filename(livrable),
                        livrable.nom(),
                        downloaded.contentType(),
                        livrable.contentType(),
                        downloaded.size(),
                        livrable.objectName(),
                        hash,
                        "RECEIVED",
                        null,
                        content,
                        base64
                );
            }
        } catch (Exception ex) {
            return unsupported(livrable, "MINIO_RETRIEVAL_FAILED");
        }
    }

    private MlDeliverableContent unsupported(DeliverableRef livrable, String reason) {
        return new MlDeliverableContent(
                livrable.id(),
                livrable.typeLivrableName(),
                sourceType(livrable, extensionOf(livrable)),
                filename(livrable),
                livrable.nom(),
                livrable.contentType(),
                livrable.contentType(),
                livrable.size(),
                livrable.objectName(),
                null,
                "UNSUPPORTED_FORMAT",
                reason,
                null,
                null
        );
    }

    private MlDeliverableContent securityRejected(DeliverableRef livrable, String reason) {
        return new MlDeliverableContent(
                livrable.id(),
                livrable.typeLivrableName(),
                sourceType(livrable, extensionOf(livrable)),
                filename(livrable),
                livrable.nom(),
                livrable.contentType(),
                livrable.contentType(),
                livrable.size(),
                livrable.objectName(),
                null,
                "SECURITY_REJECTED",
                reason,
                null,
                null
        );
    }

    private String inlineTextIfSafe(String sourceType, byte[] bytes) {
        if (!Set.of("TXT", "MARKDOWN", "HTML", "SONAR_REPORT", "COVERAGE_REPORT").contains(sourceType)) {
            return null;
        }
        try {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean validObjectName(String objectName) {
        return StringUtils.hasText(objectName)
                && (objectName.startsWith("livrables/") || objectName.startsWith("livrables-catalogue/"))
                && !objectName.contains("..")
                && !objectName.contains("\\")
                && !objectName.startsWith("/");
    }

    private String filename(DeliverableRef livrable) {
        return StringUtils.hasText(livrable.originalFileName())
                ? livrable.originalFileName()
                : livrable.nom();
    }

    private String extensionOf(DeliverableRef livrable) {
        String name = filename(livrable);
        if (!StringUtils.hasText(name) || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String sourceType(DeliverableRef livrable, String extension) {
        String name = filename(livrable).toLowerCase(Locale.ROOT);
        if ("pdf".equals(extension)) return "PDF";
        if ("docx".equals(extension)) return "DOCX";
        if ("txt".equals(extension)) return "TXT";
        if ("md".equals(extension) || "markdown".equals(extension)) return "MARKDOWN";
        if ("html".equals(extension) || "htm".equals(extension)) return "HTML";
        if ("png".equals(extension)) return "PNG";
        if ("jpg".equals(extension) || "jpeg".equals(extension)) return "JPEG";
        if ("json".equals(extension) && name.contains("sonar")) return "SONAR_REPORT";
        if (Set.of("json", "xml", "lcov").contains(extension)) return "COVERAGE_REPORT";
        return livrable.typeLivrableName() == null ? "UNKNOWN" : livrable.typeLivrableName();
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est requis par la specification de la plateforme Java : indisponible = JVM cassee.
            throw new IllegalStateException("Algorithme SHA-256 indisponible sur cette JVM.", e);
        }
    }
}
