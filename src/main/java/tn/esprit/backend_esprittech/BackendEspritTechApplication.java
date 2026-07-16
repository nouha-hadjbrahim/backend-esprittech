package tn.esprit.backend_esprittech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendEspritTechApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendEspritTechApplication.class, args);
    }

}
