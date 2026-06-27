package tn.esprit.backend_esprittech.controller.livrable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.service.livrable.LivrableService;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivrableControllerTest {

    @Mock
    private LivrableService livrableService;

    @InjectMocks
    private LivrableController controller;

    private LivrableResponse response;

    @BeforeEach
    void setUp() {
        response = new LivrableResponse(
                10L,
                42L,
                "Plateforme IoT",
                TypeLivrable.RAPPORT,
                "Rapport",
                "Version finale",
                "rapport.pdf",
                "livrables/42/rapport.pdf",
                "application/pdf",
                128L,
                null,
                7L,
                "Ali Mansour",
                LocalDateTime.now(),
                true
        );
    }

    @Test
    void upload_validPayload_shouldDelegateToService() {
        MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/pdf", "data".getBytes());
        when(livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", "Version finale", file)).thenReturn(response);

        LivrableResponse result = controller.upload(42L, TypeLivrable.RAPPORT, "Rapport", "Version finale", file);

        assertEquals(response, result);
        verify(livrableService).upload(42L, TypeLivrable.RAPPORT, "Rapport", "Version finale", file);
    }

    @Test
    void addLink_validPayload_shouldDelegateToService() {
        LivrableLinkRequest request = new LivrableLinkRequest(
                TypeLivrable.LIEN_GIT,
                "Depot Git",
                "Code source",
                "https://github.com/esprit/demo"
        );
        LivrableResponse linkResponse = new LivrableResponse(
                11L, 42L, "Plateforme IoT", TypeLivrable.LIEN_GIT, "Depot Git", "Code source",
                null, null, null, null, "https://github.com/esprit/demo", 7L, "Ali Mansour",
                LocalDateTime.now(), true
        );
        when(livrableService.addLink(42L, request)).thenReturn(linkResponse);

        LivrableResponse result = controller.addLink(42L, request);

        assertEquals(linkResponse, result);
        verify(livrableService).addLink(42L, request);
    }

    @Test
    void findByProjet_existingProject_shouldReturnLivrables() {
        when(livrableService.findByProjet(42L)).thenReturn(List.of(response));

        List<LivrableResponse> result = controller.findByProjet(42L);

        assertEquals(List.of(response), result);
        verify(livrableService).findByProjet(42L);
    }

    @Test
    void findById_existingLivrable_shouldReturnLivrable() {
        when(livrableService.findById(10L)).thenReturn(response);

        LivrableResponse result = controller.findById(10L);

        assertEquals(response, result);
        verify(livrableService).findById(10L);
    }

    @Test
    void download_fileWithContentType_shouldReturnTypedAttachment() {
        when(livrableService.download(10L)).thenReturn(new LivrableService.LivrableDownload(
                new ByteArrayInputStream("abc".getBytes()),
                "rapport.pdf",
                "application/pdf",
                3L
        ));

        var result = controller.download(10L);

        assertEquals(MediaType.APPLICATION_PDF, result.getHeaders().getContentType());
        assertEquals(3L, result.getHeaders().getContentLength());
        assertEquals("attachment; filename=\"rapport.pdf\"", result.getHeaders().getFirst("Content-Disposition"));
        assertNotNull(result.getBody());
        verify(livrableService).download(10L);
    }

    @Test
    void download_fileWithoutContentType_shouldUseOctetStream() {
        when(livrableService.download(10L)).thenReturn(new LivrableService.LivrableDownload(
                new ByteArrayInputStream("abc".getBytes()),
                "rapport.bin",
                null,
                3L
        ));

        var result = controller.download(10L);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, result.getHeaders().getContentType());
        assertEquals("attachment; filename=\"rapport.bin\"", result.getHeaders().getFirst("Content-Disposition"));
    }

    @Test
    void update_existingLivrable_shouldDelegateToService() {
        LivrableUpdateRequest request = new LivrableUpdateRequest(TypeLivrable.RAPPORT, "Rapport V2", null, null, true);
        when(livrableService.update(10L, request)).thenReturn(response);

        LivrableResponse result = controller.update(10L, request);

        assertEquals(response, result);
        verify(livrableService).update(10L, request);
    }

    @Test
    void delete_existingLivrable_shouldReturnNoContent() {
        var result = controller.delete(10L);

        assertEquals(204, result.getStatusCode().value());
        verify(livrableService).delete(10L);
    }

    @Test
    void findAllAdmin_shouldReturnActiveLivrables() {
        when(livrableService.findAllActiveForAdmin()).thenReturn(List.of(response));

        List<LivrableResponse> result = controller.findAllAdmin();

        assertEquals(List.of(response), result);
        verify(livrableService).findAllActiveForAdmin();
    }

    @Test
    void updateAdmin_existingLivrable_shouldDelegateToService() {
        LivrableUpdateRequest request = new LivrableUpdateRequest(TypeLivrable.DOCUMENTATION, "Doc", null, null, false);
        when(livrableService.update(10L, request)).thenReturn(response);

        LivrableResponse result = controller.updateAdmin(10L, request);

        assertEquals(response, result);
        verify(livrableService).update(10L, request);
    }

    @Test
    void deleteAdmin_existingLivrable_shouldReturnNoContent() {
        var result = controller.deleteAdmin(10L);

        assertEquals(204, result.getStatusCode().value());
        verify(livrableService).delete(10L);
    }
}
