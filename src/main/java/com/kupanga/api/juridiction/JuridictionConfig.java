package com.kupanga.api.juridiction;

import com.kupanga.api.immobilier.repository.BienRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(JuridictionProperties.class)
public class JuridictionConfig {

    /**
     * Au démarrage : chaque pays déjà présent en base a un profil et une règle. Sinon l'application s'arrête
     * (retirer un profil rendrait impossibles les documents des biens de ce pays).
     * Désactivé en profil test ({@code app.juridictions.verifier-biens-au-demarrage: false}) : la base de test,
     * partagée et sans Flyway, n'a pas toujours le schéma au démarrage d'un contexte ; la logique est testée à part.
     */
    @Bean
    @ConditionalOnProperty(name = "app.juridictions.verifier-biens-au-demarrage", havingValue = "true", matchIfMissing = true)
    ApplicationRunner verificationDesPaysEnBase(BienRepository bienRepository, JuridictionRegistry registry) {
        return args -> {
            List<Pays> sansProfil = bienRepository.findPaysUtilises().stream()
                    .filter(pays -> !registry.estPrisEnCharge(pays))
                    .sorted()
                    .toList();
            if (!sansProfil.isEmpty()) {
                throw new IllegalStateException(
                        "Des biens existent dans des pays sans profil kupanga.juridictions : " + sansProfil);
            }
        };
    }
}
