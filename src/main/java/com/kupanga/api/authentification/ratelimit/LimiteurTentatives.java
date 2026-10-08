package com.kupanga.api.authentification.ratelimit;

import com.kupanga.api.exception.business.TropDeTentativesException;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limite de tentatives sur les routes d'authentification (A3) : login, forgot-password, register, google.
 * Chaque appel consomme un essai ; au-delà du seuil, {@link TropDeTentativesException} (429).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimiteurTentatives {

    /** Pendant une panne du stockage, une seule erreur par minute (sinon Sentry est inondé). */
    private static final long INTERVALLE_LOG_PANNE_MS = TimeUnit.MINUTES.toMillis(1);

    private final StockageSeaux stockageSeaux;
    private final AtomicLong dernierLogPanne = new AtomicLong();

    /** Consomme un essai pour l'IP du client. */
    public void verifierIp(LimiteTentatives limite, HttpServletRequest request) {
        verifier(limite, request.getRemoteAddr());
    }

    /** Consomme un essai pour l'e-mail visé (normalisé : la casse ne permet pas de contourner la limite). */
    public void verifierEmail(LimiteTentatives limite, String email) {
        if (email == null || email.isBlank()) return;
        verifier(limite, normaliser(email));
    }

    /** Consomme un essai pour le couple (e-mail visé, IP du client). */
    public void verifierEmailEtIp(LimiteTentatives limite, String email, HttpServletRequest request) {
        if (email == null || email.isBlank()) return;
        verifier(limite, normaliser(email) + "|" + request.getRemoteAddr());
    }

    private String normaliser(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void verifier(LimiteTentatives limite, String identifiant) {
        ConsumptionProbe probe;
        try {
            probe = stockageSeaux.seau(limite.getPrefixe() + ":" + identifiant, limite.configuration())
                    .tryConsumeAndReturnRemaining(1);
        } catch (RuntimeException e) {
            // Redis indisponible : on laisse passer plutôt que de bloquer toute connexion (BCrypt reste en place)
            signalerPanne(limite, e);
            return;
        }
        if (!probe.isConsumed()) {
            long secondes = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
            log.warn("Limite de tentatives atteinte : {}", limite);
            throw new TropDeTentativesException(secondes);
        }
    }

    private void signalerPanne(LimiteTentatives limite, RuntimeException e) {
        long maintenant = System.currentTimeMillis();
        long precedent = dernierLogPanne.get();
        if (maintenant - precedent >= INTERVALLE_LOG_PANNE_MS && dernierLogPanne.compareAndSet(precedent, maintenant)) {
            log.error("Limite de tentatives indisponible ({}), requêtes autorisées : {}", limite, e.getMessage());
        } else {
            log.debug("Limite de tentatives indisponible ({}), requête autorisée", limite);
        }
    }
}
