package tn.esprit.backend_esprittech.service.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.dto.CritereEval.MlDeliverableContent;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class DeliverableContentExtractionService {

    private static final int MAX_CONTENT_CHARS = 30_000;
    private static final Pattern PDF_LITERAL_TEXT = Pattern.compile("\\(([^()]{3,500})\\)");
    private static final Pattern PRINTABLE_RUN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 .,;:!?/_()'\"-]{20,}");

    private final MinioStorageService storageService;

    @Transactional(readOnly = true)
    public List<MlDeliverableContent> extract(List<Livrable> livrables) {
        return livrables.stream()
                .map(this::extract)
                .toList();
    }

    private MlDeliverableContent extract(Livrable livrable) {
        if (!StringUtils.hasText(livrable.getObjectName())) {
            return unsupported(livrable,
                    "External links are kept for traceability; their remote content is not analysed.");
        }

        String extension = extensionOf(livrable);
        try {
            MinioStorageService.DownloadedObject downloaded = storageService.download(livrable.getObjectName());
            try (InputStream inputStream = downloaded.inputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                String content = switch (extension) {
                    case "txt" -> new String(bytes, StandardCharsets.UTF_8);
                    case "docx" -> extractDocx(bytes);
                    case "pdf" -> extractPdfText(bytes);
                    default -> "";
                };
                if (!StringUtils.hasText(content)) {
                    return unsupported(livrable,
                            "The file format is unsupported or no readable text could be extracted.");
                }
                return new MlDeliverableContent(
                        livrable.getId(),
                        livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name(),
                        livrable.getNom(),
                        livrable.getContentType(),
                        "READY",
                        null,
                        limit(cleanText(content))
                );
            }
        } catch (Exception ex) {
            return unsupported(livrable, "Content extraction failed: " + ex.getMessage());
        }
    }

    private MlDeliverableContent unsupported(Livrable livrable, String reason) {
        return new MlDeliverableContent(
                livrable.getId(),
                livrable.getTypeLivrable() == null ? null : livrable.getTypeLivrable().name(),
                livrable.getNom(),
                livrable.getContentType(),
                "UNSUPPORTED",
                reason,
                null
        );
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(readZipEntry(zip), StandardCharsets.UTF_8);
                    return xml.replaceAll("<[^>]+>", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'");
                }
            }
        }
        return "";
    }

    private byte[] readZipEntry(ZipInputStream zip) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        zip.transferTo(output);
        return output.toByteArray();
    }

    private String extractPdfText(byte[] bytes) {
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        StringBuilder builder = new StringBuilder();

        Matcher literalMatcher = PDF_LITERAL_TEXT.matcher(raw);
        while (literalMatcher.find()) {
            builder.append(literalMatcher.group(1).replace("\\)", ")").replace("\\(", "(")).append(' ');
        }

        if (builder.length() < 100) {
            Matcher printableMatcher = PRINTABLE_RUN.matcher(raw);
            while (printableMatcher.find()) {
                builder.append(printableMatcher.group()).append(' ');
            }
        }
        return builder.toString();
    }

    private String cleanText(String value) {
        return value.replaceAll("\\p{Cntrl}+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limit(String content) {
        if (content.length() <= MAX_CONTENT_CHARS) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_CHARS);
    }

    private String extensionOf(Livrable livrable) {
        String name = StringUtils.hasText(livrable.getOriginalFileName())
                ? livrable.getOriginalFileName()
                : livrable.getObjectName();
        if (!StringUtils.hasText(name) || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
