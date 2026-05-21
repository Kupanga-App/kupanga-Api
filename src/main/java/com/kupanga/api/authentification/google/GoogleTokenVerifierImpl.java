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

            GoogleIdToken.Payload payload = idToken.getPayload();

            String firstName = (String) payload.get("given_name");
            String lastName  = (String) payload.get("family_name");
            String picture   = (String) payload.get("picture");

            log.info("[GOOGLE-AUTH] Token vérifié pour {}", payload.getEmail());

            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    firstName  != null ? firstName : "",
                    lastName   != null ? lastName  : "",
                    picture
            );

        } catch (KupangaBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GOOGLE-AUTH] Erreur de vérification : {}", e.getMessage());
            throw new KupangaBusinessException("Erreur lors de la vérification du token Google", HttpStatus.UNAUTHORIZED);
        }
    }
}
