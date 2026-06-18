package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtPropertiesTest {

    @Test
    void gettersAndSetters() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("my-secret");
        properties.setExpirationMs(900000);

        assertEquals("my-secret", properties.getSecret());
        assertEquals(900000, properties.getExpirationMs());
    }
}
