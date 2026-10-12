package com.kupanga.api.authentification.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Limites de tentatives appliquées avant la résolution des arguments du contrôleur (B3).
 * {@link ObjectProvider} : les tests {@code @WebMvcTest} chargent cette configuration sans le limiteur ;
 * l'intercepteur n'est alors pas enregistré (il l'est dès qu'un {@link LimiteurTentatives}, réel ou simulé, existe).
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<LimiteurTentatives> limiteurTentatives;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        limiteurTentatives.ifAvailable(limiteur ->
                registry.addInterceptor(new LimiteInscriptionInterceptor(limiteur)).addPathPatterns("/auth/register"));
    }
}
