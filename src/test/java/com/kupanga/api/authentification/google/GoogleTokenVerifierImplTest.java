package com.kupanga.api.authentification.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kupanga.api.exception.business.KupangaBusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.kupanga.api.authentification.constant.AuthConstant.ADRESSE_GOOGLE_NON_VERIFIEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests unitaires pour GoogleTokenVerifierImpl (A4)")
class GoogleTokenVerifierImplTest {

    private final GoogleTokenVerifierImpl verifier = new GoogleTokenVerifierImpl();

    private static GoogleIdToken.Payload payload(String email, Boolean emailVerifie) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("g-123")
                .setEmail(email)
                .setEmailVerified(emailVerifie);
        payload.set("given_name", "Alice");
        payload.set("family_name", "Martin");
        payload.set("picture", "https://lh3.googleusercontent.com/photo");
        return payload;
    }

    @Test
    @DisplayName("Adresse confirmée par Google → informations extraites")
    void adresseConfirmee_acceptee() {
        GoogleUserInfo info = verifier.extraire(payload("alice@gmail.com", true));

        assertThat(info).isEqualTo(new GoogleUserInfo("g-123", "alice@gmail.com", "Alice", "Martin",
                "https://lh3.googleusercontent.com/photo"));
    }

    @Test
    @DisplayName("email_verified à false ou absent → 401, aucune information renvoyée")
    void adresseNonConfirmee_refusee() {
        for (Boolean emailVerifie : new Boolean[]{false, null}) {
            assertThatThrownBy(() -> verifier.extraire(payload("victime@example.com", emailVerifie)))
                    .isInstanceOfSatisfying(KupangaBusinessException.class, e -> {
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        assertThat(e.getMessage()).isEqualTo(ADRESSE_GOOGLE_NON_VERIFIEE);
                    });
        }
    }

    @Test
    @DisplayName("Jeton sans adresse e-mail → 401")
    void sansAdresse_refuse() {
        for (String email : new String[]{null, " "}) {
            assertThatThrownBy(() -> verifier.extraire(payload(email, true)))
                    .isInstanceOf(KupangaBusinessException.class);
        }
    }
}
