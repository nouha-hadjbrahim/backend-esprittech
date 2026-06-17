package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DotenvConfigTest {

    @Test
    void gettersAndSetters() {
        DotenvConfig config = new DotenvConfig();
        config.setSecret("my-secret");
        config.setAccessExpiration(900000);
        config.setRefreshExpiration(604800000);

        assertEquals("my-secret", config.getSecret());
        assertEquals(900000, config.getAccessExpiration());
        assertEquals(604800000, config.getRefreshExpiration());
    }
}
