package tn.esprit.backend_esprittech.dto.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageResponseTest {

    @Test
    void of_createsWithTimestamp() {
        MessageResponse response = MessageResponse.of("hello");

        assertEquals("hello", response.message());
        assertNotNull(response.timestamp());
    }
}
