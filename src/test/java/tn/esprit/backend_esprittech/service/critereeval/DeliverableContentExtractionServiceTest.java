package tn.esprit.backend_esprittech.service.critereeval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverableContentExtractionServiceTest {

    @Mock
    private MinioStorageService storageService;

    @InjectMocks
    private DeliverableContentExtractionService extractionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(extractionService, "maxFileBytes", 5_242_880L);
    }

    @Test
    void extract_marksSignatureMismatchAsCorruptedContent() {
        Livrable livrable = new Livrable();
        livrable.setId(7L);
        livrable.setObjectName("livrables/report.pdf");
        livrable.setOriginalFileName("report.pdf");
        livrable.setNom("report.pdf");
        livrable.setSize(12L);

        when(storageService.download("livrables/report.pdf")).thenReturn(new MinioStorageService.DownloadedObject(
                new ByteArrayInputStream("not-a-pdf".getBytes(StandardCharsets.UTF_8)),
                "application/pdf",
                12L
        ));

        var result = extractionService.extract(List.of(livrable));

        assertEquals(1, result.size());
        assertEquals("RECEIVED", result.get(0).extractionStatus());
        assertNull(result.get(0).unsupportedReason());
        assertNotNull(result.get(0).contentBase64());
    }

    // ---- Rejets de securite et formats non supportes ----

    @Test
    void extract_objectOutsideAllowedPrefix_shouldBeSecurityRejected() {
        var result = extractionService.extract(List.of(livrable("secrets/passwd.pdf", "passwd.pdf", 10L)));

        assertEquals("SECURITY_REJECTED", result.get(0).extractionStatus());
        assertEquals("INVALID_MINIO_OBJECT_NAME", result.get(0).unsupportedReason());
    }

    @Test
    void extract_objectNameWithTraversal_shouldBeSecurityRejected() {
        var result = extractionService.extract(List.of(livrable("livrables/../secrets.pdf", "s.pdf", 10L)));

        assertEquals("SECURITY_REJECTED", result.get(0).extractionStatus());
        assertEquals("INVALID_MINIO_OBJECT_NAME", result.get(0).unsupportedReason());
    }

    @Test
    void extract_declaredSizeAboveLimit_shouldBeSecurityRejected() {
        var result = extractionService.extract(
                List.of(livrable("livrables/gros.pdf", "gros.pdf", 10_000_000L)));

        assertEquals("SECURITY_REJECTED", result.get(0).extractionStatus());
        assertEquals("FILE_SIZE_LIMIT_EXCEEDED", result.get(0).unsupportedReason());
    }

    @Test
    void extract_unsupportedExtension_shouldBeReported() {
        var result = extractionService.extract(List.of(livrable("livrables/archive.zip", "archive.zip", 10L)));

        assertEquals("UNSUPPORTED_FORMAT", result.get(0).extractionStatus());
        assertEquals("UNSUPPORTED_EXTENSION", result.get(0).unsupportedReason());
    }

    @Test
    void extract_downloadedObjectAboveLimit_shouldBeSecurityRejected() {
        when(storageService.download("livrables/report.pdf")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)),
                        "application/pdf", 10_000_000L));

        var result = extractionService.extract(List.of(livrable("livrables/report.pdf", "report.pdf", null)));

        assertEquals("SECURITY_REJECTED", result.get(0).extractionStatus());
        assertEquals("MINIO_OBJECT_SIZE_LIMIT_EXCEEDED", result.get(0).unsupportedReason());
    }

    @Test
    void extract_storageFailure_shouldBeReportedAsRetrievalFailure() {
        when(storageService.download("livrables/report.pdf"))
                .thenThrow(new IllegalStateException("minio down"));

        var result = extractionService.extract(List.of(livrable("livrables/report.pdf", "report.pdf", 10L)));

        assertEquals("UNSUPPORTED_FORMAT", result.get(0).extractionStatus());
        assertEquals("MINIO_RETRIEVAL_FAILED", result.get(0).unsupportedReason());
    }

    @Test
    void extract_deliverableWithoutObjectName_shouldBeSkipped() {
        Livrable sansObjet = new Livrable();
        sansObjet.setId(1L);
        sansObjet.setNom("Lien externe");

        assertTrue(extractionService.extract(List.of(sansObjet)).isEmpty());
    }

    // ---- Determination du type de source ----

    @Test
    void extract_textFile_shouldInlineContent() {
        when(storageService.download("livrables/notes.txt")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream("bonjour".getBytes(StandardCharsets.UTF_8)),
                        "text/plain", 7L));

        var result = extractionService.extract(List.of(livrable("livrables/notes.txt", "notes.txt", 7L)));

        assertEquals("TXT", result.get(0).sourceType());
        assertEquals("bonjour", result.get(0).content());
    }

    @Test
    void extract_sonarJsonReport_shouldBeTypedAsSonarReport() {
        when(storageService.download("livrables/sonar-report.json")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)),
                        "application/json", 2L));

        var result = extractionService.extract(
                List.of(livrable("livrables/sonar-report.json", "sonar-report.json", 2L)));

        assertEquals("SONAR_REPORT", result.get(0).sourceType());
        assertEquals("{}", result.get(0).content());
    }

    @Test
    void extract_xmlReport_shouldBeTypedAsCoverageReport() {
        when(storageService.download("livrables/jacoco.xml")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream("<r/>".getBytes(StandardCharsets.UTF_8)),
                        "application/xml", 4L));

        var result = extractionService.extract(List.of(livrable("livrables/jacoco.xml", "jacoco.xml", 4L)));

        assertEquals("COVERAGE_REPORT", result.get(0).sourceType());
    }

    @Test
    void extract_imageFile_shouldNotInlineContent() {
        when(storageService.download("livrables/schema.png")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream(new byte[]{1, 2, 3}),
                        "image/png", 3L));

        var result = extractionService.extract(List.of(livrable("livrables/schema.png", "schema.png", 3L)));

        assertEquals("PNG", result.get(0).sourceType());
        assertNull(result.get(0).content());
        assertNotNull(result.get(0).contentBase64());
    }

    @Test
    void extract_catalogueDeliverables_shouldUseSameRules() {
        LivrableCatalogue livrable = new LivrableCatalogue();
        livrable.setId(3L);
        livrable.setObjectName("livrables-catalogue/100/guide.md");
        livrable.setOriginalFileName("guide.md");
        livrable.setNom("Guide");
        livrable.setSize(5L);
        when(storageService.download("livrables-catalogue/100/guide.md")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream("# Doc".getBytes(StandardCharsets.UTF_8)),
                        "text/markdown", 5L));

        var result = extractionService.extractCatalogue(List.of(livrable));

        assertEquals("MARKDOWN", result.get(0).sourceType());
        assertEquals("# Doc", result.get(0).content());
    }

    private Livrable livrable(String objectName, String fileName, Long size) {
        Livrable livrable = new Livrable();
        livrable.setId(7L);
        livrable.setObjectName(objectName);
        livrable.setOriginalFileName(fileName);
        livrable.setNom(fileName);
        livrable.setSize(size);
        return livrable;
    }
}
