package com.kupanga.api.authentification.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.kupanga.api.authentification.ratelimit.LimiteTentatives.REGISTER_PAR_IP;

/**
 * Limite {@link LimiteTentatives#REGISTER_PAR_IP} vérifiée avant la lecture du corps multipart (B3) :
 * l'analyse est différée ({@code resolve-lazily}), un client bloqué ne peut donc plus faire stocker
 * jusqu'à 50 Mo de fichiers temporaires à chaque tentative. Enregistré par {@link RateLimitWebConfig}.
 */
@RequiredArgsConstructor
public class LimiteInscriptionInterceptor implements HandlerInterceptor {

    private final LimiteurTentatives limiteurTentatives;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        limiteurTentatives.verifierIp(REGISTER_PAR_IP, request);
        return true;
    }
}
