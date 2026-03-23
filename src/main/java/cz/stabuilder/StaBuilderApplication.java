package cz.stabuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StaBuilderApplication {

    public static void main(String[] args) {
        SpringApplication.run(StaBuilderApplication.class, args);
        System.out.println("\n=== STA Builder běží na http://localhost:8080 ===\n");
    }
}
