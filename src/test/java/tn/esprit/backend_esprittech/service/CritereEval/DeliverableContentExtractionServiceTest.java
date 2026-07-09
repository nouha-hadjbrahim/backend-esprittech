package tn.esprit.backend_esprittech.service.CritereEval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverableContentExtractionServiceTest {

    @Mock
    private MinioStorageService storageService;

    @InjectMocks
    private DeliverableContentExtractionService extractionService;

    @Test
    void extract_marksSignatureMismatchAsCorruptedContent() throws Exception {
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
        assertEquals("CORRUPTED_CONTENT", result.get(0).extractionStatus());
        assertEquals("CONTENT_SIGNATURE_MISMATCH", result.get(0).unsupportedReason());
        assertNull(result.get(0).contentBase64());
    }
}
