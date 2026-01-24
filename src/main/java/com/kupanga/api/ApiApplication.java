package com.kupanga.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application.
 */
@SpringBootApplication(scanBasePackages = "com.kupanga.api")
public class ApiApplication {

        public static void main(final String[] args) {
                SpringApplication.run(ApiApplication.class, args);
        }

}
