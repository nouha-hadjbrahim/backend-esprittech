package tn.esprit.backend_esprittech.controller.critereeval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteResponse;
import tn.esprit.backend_esprittech.service.critereeval.CritereNoteService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CritereNoteControllerTest {

    @Mock private CritereNoteService critereNoteService;

    @InjectMocks private CritereNoteController controller;

    private CritereNoteResponse response;
    private CritereNoteRequest request;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        response = new CritereNoteResponse(
                1L, "Note Criteria", "Description", "TECHNIQUE", 1, true, now, now, 10.0, 5.0, 8.0
        );
        request = new CritereNoteRequest("Note Criteria", "Description", "TECHNIQUE", 1, 10.0, 5.0, 8.0, true);
    }

    @Test
    void findAll_returnsList_withOkStatus() {
        List<CritereNoteResponse> responses = List.of(response);
        when(critereNoteService.findAll()).thenReturn(responses);

        ResponseEntity<List<CritereNoteResponse>> result = controller.findAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals(response, result.getBody().get(0));
        verify(critereNoteService).findAll();
    }

    @Test
    void findAll_emptyList_returnsEmptyListWithOkStatus() {
        when(critereNoteService.findAll()).thenReturn(new ArrayList<>());

        ResponseEntity<List<CritereNoteResponse>> result = controller.findAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
        verify(critereNoteService).findAll();
    }

    @Test
    void findActive_returnsList_withOkStatus() {
        List<CritereNoteResponse> responses = List.of(response);
        when(critereNoteService.findActive()).thenReturn(responses);

        ResponseEntity<List<CritereNoteResponse>> result = controller.findActive();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals(response, result.getBody().get(0));
        verify(critereNoteService).findActive();
    }

    @Test
    void findActive_emptyList_returnsEmptyListWithOkStatus() {
        when(critereNoteService.findActive()).thenReturn(new ArrayList<>());

        ResponseEntity<List<CritereNoteResponse>> result = controller.findActive();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
        verify(critereNoteService).findActive();
    }

    @Test
    void create_validRequest_returnsCreatedResponseWithOkStatus() {
        when(critereNoteService.create(request)).thenReturn(response);

        ResponseEntity<CritereNoteResponse> result = controller.create(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(critereNoteService).create(request);
    }

    @Test
    void update_validRequest_returnsUpdatedResponseWithOkStatus() {
        when(critereNoteService.update(1L, request)).thenReturn(response);

        ResponseEntity<CritereNoteResponse> result = controller.update(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(critereNoteService).update(1L, request);
    }

    @Test
    void deactivate_callsService_returnsNoContent() {
        ResponseEntity<Void> result = controller.deactivate(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(critereNoteService).deactivate(1L);
    }

    @Test
    void activate_callsService_returnsNoContent() {
        ResponseEntity<Void> result = controller.activate(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(critereNoteService).activate(1L);
    }

    @Test
    void delete_callsService_returnsNoContent() {
        ResponseEntity<Void> result = controller.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(critereNoteService).delete(1L);
    }

    @Test
    void create_withDifferentValues_returnsResponse() {
        CritereNoteRequest newRequest = new CritereNoteRequest("Another Note", "Another Desc", "SECURITE", 2, 20.0, 10.0, 15.0, false);
        CritereNoteResponse newResponse = new CritereNoteResponse(
                2L, "Another Note", "Another Desc", "SECURITE", 2, false, LocalDateTime.now(), LocalDateTime.now(), 20.0, 10.0, 15.0
        );
        when(critereNoteService.create(newRequest)).thenReturn(newResponse);

        ResponseEntity<CritereNoteResponse> result = controller.create(newRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(newResponse, result.getBody());
    }

    @Test
    void update_withDifferentId_callsServiceWithCorrectId() {
        Long testId = 999L;
        when(critereNoteService.update(testId, request)).thenReturn(response);

        controller.update(testId, request);

        verify(critereNoteService).update(testId, request);
    }

    @Test
    void findAll_multipleResponses_returnsAllResponses() {
        LocalDateTime now = LocalDateTime.now();
        List<CritereNoteResponse> responses = List.of(
                new CritereNoteResponse(1L, "Note1", "Desc1", "TECHNIQUE", 1, true, now, now, 10.0, 5.0, 8.0),
                new CritereNoteResponse(2L, "Note2", "Desc2", "SECURITE", 2, true, now, now, 20.0, 10.0, 15.0),
                new CritereNoteResponse(3L, "Note3", "Desc3", "INNOVATION", 3, false, now, now, 15.0, 7.0, 12.0)
        );
        when(critereNoteService.findAll()).thenReturn(responses);

        ResponseEntity<List<CritereNoteResponse>> result = controller.findAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().size());
    }

    @Test
    void deactivate_multipleIds_callsServiceForEach() {
        controller.deactivate(1L);
        controller.deactivate(2L);
        controller.deactivate(3L);

        verify(critereNoteService).deactivate(1L);
        verify(critereNoteService).deactivate(2L);
        verify(critereNoteService).deactivate(3L);
    }

    @Test
    void activate_multipleIds_callsServiceForEach() {
        controller.activate(1L);
        controller.activate(2L);

        verify(critereNoteService).activate(1L);
        verify(critereNoteService).activate(2L);
    }
}
