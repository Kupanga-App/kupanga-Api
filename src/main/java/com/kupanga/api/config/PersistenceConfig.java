package com.kupanga.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.kupanga.api.utilisateur.repository",
        "com.kupanga.api.login.repository"
})
@EntityScan(basePackages = {
        "com.kupanga.api.utilisateur.entity",
        "com.kupanga.api.immobilier.entity",
        "com.kupanga.api.chat.entity",
        "com.kupanga.api.login.entity"
})
public class PersistenceConfig {
}
