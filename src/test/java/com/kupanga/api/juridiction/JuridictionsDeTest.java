package com.kupanga.api.juridiction;

import com.kupanga.api.juridiction.regle.RegleFrance;
import com.kupanga.api.juridiction.regle.RegleRdc;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Aide de test : profils et plafonds réellement chargés depuis {@code application.yml}, comme au démarrage.
 * Permet aux tests unitaires des services d'utiliser le vrai {@link JuridictionRegistry} (plafonds, devises,
 * version des modèles) plutôt qu'un bouchon.
 */
public final class JuridictionsDeTest {

    private JuridictionsDeTest() {
    }

    /** Propriétés {@code kupanga.*} de {@code application.yml}. */
    public static JuridictionProperties proprietesDuYaml() {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                    .load("application", new ClassPathResource("application.yml"));
            StandardEnvironment env = new StandardEnvironment();
            sources.forEach(env.getPropertySources()::addLast);
            return new Binder(ConfigurationPropertySources.get(env))
                    .bind("kupanga", JuridictionProperties.class).get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Registre réel (FR + CD) construit depuis {@code application.yml}. */
    public static JuridictionRegistry registre() {
        return new JuridictionRegistry(proprietesDuYaml(), List.of(new RegleFrance(), new RegleRdc()));
    }
}
