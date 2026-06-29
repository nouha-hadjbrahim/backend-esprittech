package tn.esprit.backend_esprittech.controller.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.ReferenceItemRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetReferenceService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SujetReferenceAdminControllerTest {

    @Mock
    private SujetReferenceService referenceService;

    private SujetReferenceAdminController controller;
    private ReferenceItemResponse item;

    @BeforeEach
    void setUp() {
        controller = new SujetReferenceAdminController(referenceService);
        item = new ReferenceItemResponse(1L, "Cloud", LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    @Test
    void domaineEndpoints_shouldDelegateToReferenceService() {
        PagedResponse<ReferenceItemResponse> page = page(item);
        ReferenceItemRequest request = new ReferenceItemRequest("Cloud");
        when(referenceService.getDomaines(0, 12, "cl")).thenReturn(page);
        when(referenceService.createDomaine(request)).thenReturn(item);
        when(referenceService.updateDomaine(1L, request)).thenReturn(item);

        assertEquals(page, controller.getDomaines(0, 12, "cl").getBody());
        assertEquals(HttpStatus.CREATED, controller.createDomaine(request).getStatusCode());
        assertEquals(item, controller.updateDomaine(1L, request).getBody());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteDomaine(1L).getStatusCode());

        verify(referenceService).deleteDomaine(1L);
    }

    @Test
    void prerequisEndpoints_shouldDelegateToReferenceService() {
        PagedResponse<ReferenceItemResponse> page = page(item);
        ReferenceItemRequest request = new ReferenceItemRequest("Java");
        when(referenceService.getPrerequis(1, 5, "ja")).thenReturn(page);
        when(referenceService.createPrerequis(request)).thenReturn(item);
        when(referenceService.updatePrerequis(2L, request)).thenReturn(item);

        assertEquals(page, controller.getPrerequis(1, 5, "ja").getBody());
        assertEquals(HttpStatus.CREATED, controller.createPrerequis(request).getStatusCode());
        assertEquals(item, controller.updatePrerequis(2L, request).getBody());
        assertEquals(HttpStatus.NO_CONTENT, controller.deletePrerequis(2L).getStatusCode());

        verify(referenceService).deletePrerequis(2L);
    }

    @Test
    void technologieEndpointsAndCounts_shouldDelegateToReferenceService() {
        PagedResponse<ReferenceItemResponse> page = page(item);
        ReferenceItemRequest request = new ReferenceItemRequest("Angular");
        when(referenceService.getTechnologies(2, 20, null)).thenReturn(page);
        when(referenceService.createTechnologie(request)).thenReturn(item);
        when(referenceService.updateTechnologie(3L, request)).thenReturn(item);
        when(referenceService.countDomaines()).thenReturn(4L);
        when(referenceService.countPrerequis()).thenReturn(5L);
        when(referenceService.countTechnologies()).thenReturn(6L);

        assertEquals(page, controller.getTechnologies(2, 20, null).getBody());
        assertEquals(HttpStatus.CREATED, controller.createTechnologie(request).getStatusCode());
        assertEquals(item, controller.updateTechnologie(3L, request).getBody());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteTechnologie(3L).getStatusCode());

        var counts = controller.getCounts().getBody();
        assertEquals(4L, counts.domaines());
        assertEquals(5L, counts.prerequis());
        assertEquals(6L, counts.technologies());
        verify(referenceService).deleteTechnologie(3L);
    }

    private PagedResponse<ReferenceItemResponse> page(ReferenceItemResponse item) {
        return new PagedResponse<>(List.of(item), 0, 12, 1, 1, true, true);
    }
}
