package no.nav.skanmotovrig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

@Import(value = {ApplicationConfig.class})
@EnableScheduling
@EnableResilientMethods
@SpringBootApplication
public class Application {
    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
