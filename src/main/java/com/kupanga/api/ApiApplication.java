package com.kupanga.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Point d'entrée de l'application.
 */
@SpringBootApplication(scanBasePackages = "com.kupanga.api")
@EnableJpaRepositories(basePackages = {
        "com.kupanga.api.utilisateur.repository"
})
@EntityScan(basePackages = {
        "com.kupanga.api.utilisateur.entity",
        "com.kupanga.api.immobilier.entity",
        "com.kupanga.api.chat.entity"
})
public class ApiApplication {

	public static void main(final String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
