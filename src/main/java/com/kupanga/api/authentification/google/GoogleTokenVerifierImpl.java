package com.kupanga.api.authentification.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.kupanga.api.exception.business.KupangaBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;

import static com.kupanga.api.authentification.constant.AuthConstant.ADRESSE_GOOGLE_NON_VERIFIEE;

@Component
@Slf4j
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    @Value("${google.client-id}")
    private String clientId;

    @Override
    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new KupangaBusinessException("Token Google invalide ou expiré", HttpStatus.UNAUTHORIZED);
            }

            return extraire(idToken.getPayload());

        } catch (KupangaBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[GOOGLE-AUTH] Erreur de vérification : {}", e.getClass().getSimpleName());
            throw new KupangaBusinessException("Erreur lors de la vérification du token Google", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * A4 : n'accepte que les adresses que Google a confirmées ({@code email_verified}). Sans ce contrôle, un compte
     * Google ouvert avec l'adresse d'autrui (non confirmée) se connecterait au compte Kupanga de cette adresse.
     */
    GoogleUserInfo extraire(GoogleIdToken.Payload payload) {
        if (payload.getEmail() == null || payload.getEmail().isBlank()
                || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            log.info("[GOOGLE-AUTH] Jeton refusé : adresse e-mail non confirmée par Google");
            throw new KupangaBusinessException(ADRESSE_GOOGLE_NON_VERIFIEE, HttpStatus.UNAUTHORIZED);
        }

        String firstName = (String) payload.get("given_name");
        String lastName  = (String) payload.get("family_name");
        String picture   = (String) payload.get("picture");

        log.debug("[GOOGLE-AUTH] Jeton vérifié");

        return new GoogleUserInfo(
                payload.getSubject(),
                payload.getEmail(),
                firstName  != null ? firstName : "",
                lastName   != null ? lastName  : "",
                picture
        );
    }
}
