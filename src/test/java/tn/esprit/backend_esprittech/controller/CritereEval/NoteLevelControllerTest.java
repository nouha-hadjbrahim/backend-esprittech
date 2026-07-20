package tn.esprit.backend_esprittech.controller.CritereEval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tn.esprit.backend_esprittech.dto.CritereEval.NoteLevelRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.NoteLevelResponse;
import tn.esprit.backend_esprittech.service.CritereEval.NoteLevelService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteLevelControllerTest {

    @Mock
    private NoteLevelService service;

    @InjectMocks
    private NoteLevelController controller;

    @Test
    void findAll_shouldReturnServiceLevels() {
        NoteLevelResponse response = response();
        when(service.findAll()).thenReturn(List.of(response));

        List<NoteLevelResponse> result = controller.findAll();

        assertEquals(List.of(response), result);
    }

    @Test
    void create_shouldDelegateRequest() {
        NoteLevelRequest request = request();
        NoteLevelResponse response = response();
        when(service.create(request)).thenReturn(response);

        NoteLevelResponse result = controller.create(request);

        assertEquals(response, result);
        verify(service).create(request);
    }

    @Test
    void update_shouldDelegateRequest() {
        NoteLevelRequest request = request();
        NoteLevelResponse response = response();
        when(service.update(1L, request)).thenReturn(response);

        NoteLevelResponse result = controller.update(1L, request);

        assertEquals(response, result);
        verify(service).update(1L, request);
    }

    @Test
    void activate_shouldReturnNoContent() {
        var result = controller.activate(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(service).activate(1L);
    }

    @Test
    void deactivate_shouldReturnNoContent() {
        var result = controller.deactivate(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(service).deactivate(1L);
    }

    @Test
    void delete_shouldReturnNoContent() {
        var result = controller.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(service).delete(1L);
    }

    private NoteLevelRequest request() {
        return new NoteLevelRequest(5, "Excellent", "Stable", true, 1);
    }

    private NoteLevelResponse response() {
        LocalDateTime now = LocalDateTime.now();
        return new NoteLevelResponse(1L, 5, "Excellent", "Stable", true, 1, now, now);
    }
}
