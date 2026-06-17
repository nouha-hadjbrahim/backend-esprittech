package tn.esprit.backend_esprittech;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class BackendEspritTechApplicationTests {

    @Test
    void main_startsApplication() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(
                    eq(BackendEspritTechApplication.class), any(String[].class)))
                    .thenReturn(null);

            BackendEspritTechApplication.main(new String[]{});

            mocked.verify(() -> SpringApplication.run(
                    eq(BackendEspritTechApplication.class), any(String[].class)));
        }
    }
}
