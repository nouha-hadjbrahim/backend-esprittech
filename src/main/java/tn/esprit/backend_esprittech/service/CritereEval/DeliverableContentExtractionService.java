package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.dto.CritereEval.MlDeliverableContent;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.InputStream;
import java.security.MessageDigest;
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

    @Transactional(readOnly = true)
    public List<MlDeliverableContent> extract(List<Livrable> livrables) {
        return livrables.stream()
                .filter(livrable -> StringUtils.hasText(livrable.getObjectName()))
                .map(this::prepare)
                .toList();
    }

    private MlDeliverableContent prepare(Livrable livrable) {
        if (!StringUtils.hasText(livrable.getObjectName())) {
            return unsupported(livrable, "EXTERNAL_LINK_NOT_A_MINIO_OBJECT");
        }
        if (!validObjectName(livrable.getObjectName())) {
            return securityRejected(livrable, "INVALID_MINIO_OBJECT_NAME");
        }
        if (livrable.getSize() != null && livrable.getSize() > maxFileBytes) {
            return securityRejected(livrable, "FILE_SIZE_LIMIT_EXCEEDED");
        }
        String extension = extensionOf(livrable);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            return unsupported(livrable, "UNSUPPORTED_EXTENSION");
        }

        try {
            MinioStorageService.DownloadedObject downloaded = storageService.download(livrable.getObjectName());
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
                        livrable.getId(),
                        livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name(),
                        sourceType,
                        filename(livrable),
                        livrable.getNom(),
                        downloaded.contentType(),
                        livrable.getContentType(),
                        downloaded.size(),
                        livrable.getObjectName(),
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

    private MlDeliverableContent unsupported(Livrable livrable, String reason) {
        return new MlDeliverableContent(
                livrable.getId(),
                livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name(),
                sourceType(livrable, extensionOf(livrable)),
                filename(livrable),
                livrable.getNom(),
                livrable.getContentType(),
                livrable.getContentType(),
                livrable.getSize(),
                livrable.getObjectName(),
                null,
                "UNSUPPORTED_FORMAT",
                reason,
                null,
                null
        );
    }

    private MlDeliverableContent securityRejected(Livrable livrable, String reason) {
        return new MlDeliverableContent(
                livrable.getId(),
                livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name(),
                sourceType(livrable, extensionOf(livrable)),
                filename(livrable),
                livrable.getNom(),
                livrable.getContentType(),
                livrable.getContentType(),
                livrable.getSize(),
                livrable.getObjectName(),
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
                && objectName.startsWith("livrables/")
                && !objectName.contains("..")
                && !objectName.contains("\\")
                && !objectName.startsWith("/");
    }

    private String filename(Livrable livrable) {
        return StringUtils.hasText(livrable.getOriginalFileName())
                ? livrable.getOriginalFileName()
                : livrable.getNom();
    }

    private String extensionOf(Livrable livrable) {
        String name = filename(livrable);
        if (!StringUtils.hasText(name) || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String sourceType(Livrable livrable, String extension) {
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
        return livrable.getTypeLivrable() == null ? "UNKNOWN" : livrable.getTypeLivrable().name();
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
