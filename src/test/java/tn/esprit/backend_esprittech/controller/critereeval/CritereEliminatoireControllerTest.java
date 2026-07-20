package tn.esprit.backend_esprittech.controller.critereeval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.critereeval.CritereEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CritereEliminatoireResponse;
import tn.esprit.backend_esprittech.dto.critereeval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.service.critereeval.CritereEliminatoireService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CritereEliminatoireControllerTest {

    @Mock private CritereEliminatoireService service;

    @InjectMocks private CritereEliminatoireController controller;

    private CritereEliminatoireResponse response;
    private CritereEliminatoireRequest request;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        response = new CritereEliminatoireResponse(
                1L, "Libelle Test", "Description", "TECHNIQUE", 1, true, now, now, null
        );
        request = new CritereEliminatoireRequest("Libelle Test", "Description", "TECHNIQUE", 1, null, true);
    }

    @Test
    void findAll_returnsList() {
        List<CritereEliminatoireResponse> responses = List.of(response);
        when(service.findAll()).thenReturn(responses);

        List<CritereEliminatoireResponse> result = controller.findAll();

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
        verify(service).findAll();
    }

    @Test
    void findAll_emptyList_returnsEmptyList() {
        when(service.findAll()).thenReturn(new ArrayList<>());

        List<CritereEliminatoireResponse> result = controller.findAll();

        assertTrue(result.isEmpty());
        verify(service).findAll();
    }

    @Test
    void findActive_returnsList() {
        List<CritereEliminatoireResponse> responses = List.of(response);
        when(service.findActive()).thenReturn(responses);

        List<CritereEliminatoireResponse> result = controller.findActive();

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));
        verify(service).findActive();
    }

    @Test
    void findActive_emptyList_returnsEmptyList() {
        when(service.findActive()).thenReturn(new ArrayList<>());

        List<CritereEliminatoireResponse> result = controller.findActive();

        assertTrue(result.isEmpty());
        verify(service).findActive();
    }

    @Test
    void create_validRequest_returnsResponse() {
        when(service.create(request)).thenReturn(response);

        CritereEliminatoireResponse result = controller.create(request);

        assertEquals(response, result);
        verify(service).create(request);
    }

    @Test
    void update_validRequest_returnsUpdatedResponse() {
        when(service.update(1L, request)).thenReturn(response);

        CritereEliminatoireResponse result = controller.update(1L, request);

        assertEquals(response, result);
        verify(service).update(1L, request);
    }

    @Test
    void deactivate_callsService() {
        controller.deactivate(1L);

        verify(service).deactivate(1L);
    }

    @Test
    void activate_callsService() {
        controller.activate(1L);

        verify(service).activate(1L);
    }

    @Test
    void reorder_withValidRequests_callsService() {
        List<OrdreCritereRequest> requests = List.of(
                new OrdreCritereRequest(1L, 1),
                new OrdreCritereRequest(2L, 2)
        );

        controller.reorder(requests);

        verify(service).reorder(requests);
    }

    @Test
    void reorder_withMultipleRequests_callsService() {
        List<OrdreCritereRequest> requests = List.of(
                new OrdreCritereRequest(1L, 5),
                new OrdreCritereRequest(2L, 3),
                new OrdreCritereRequest(3L, 1)
        );

        controller.reorder(requests);

        verify(service).reorder(requests);
    }

    @Test
    void create_withDifferentValues_returnsResponse() {
        CritereEliminatoireRequest newRequest = new CritereEliminatoireRequest(
                "Another Libelle", "Another Description", "SECURITE", 2, null, false
        );
        CritereEliminatoireResponse newResponse = new CritereEliminatoireResponse(
                2L, "Another Libelle", "Another Description", "SECURITE", 2, false, LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(service.create(newRequest)).thenReturn(newResponse);

        CritereEliminatoireResponse result = controller.create(newRequest);

        assertEquals(newResponse, result);
        verify(service).create(newRequest);
    }

    @Test
    void update_withDifferentId_callsServiceWithCorrectId() {
        Long testId = 999L;
        when(service.update(testId, request)).thenReturn(response);

        controller.update(testId, request);

        verify(service).update(testId, request);
    }

    @Test
    void findAll_multipleResponses_returnsAllResponses() {
        LocalDateTime now = LocalDateTime.now();
        List<CritereEliminatoireResponse> responses = List.of(
                new CritereEliminatoireResponse(1L, "Test1", "Desc1", "TECHNIQUE", 1, true, now, now, null),
                new CritereEliminatoireResponse(2L, "Test2", "Desc2", "SECURITE", 2, true, now, now, null),
                new CritereEliminatoireResponse(3L, "Test3", "Desc3", "INNOVATION", 3, false, now, now, null)
        );
        when(service.findAll()).thenReturn(responses);

        List<CritereEliminatoireResponse> result = controller.findAll();

        assertEquals(3, result.size());
        verify(service).findAll();
    }
}
