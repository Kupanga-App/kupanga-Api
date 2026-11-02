package com.kupanga.api.email.envoi;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * M1 : identité de l'expéditeur des e-mails ({@code email.*}). Le serveur SMTP, lui, se règle par
 * {@code spring.mail.*}.
 *
 * @param expediteur    adresse d'envoi, sur un domaine dont SPF, DKIM et DMARC autorisent le serveur SMTP
 * @param nomExpediteur nom affiché
 * @param repondreA     adresse de réponse facultative ({@code Reply-To})
 */
@Validated
@ConfigurationProperties(prefix = "email")
public record EmailProperties(
        @NotBlank @Email String expediteur,
        @NotBlank String nomExpediteur,
        @Email String repondreA
) {

    /** Domaine de l'expéditeur, utilisé pour les {@code Message-ID}. */
    public String domaine() {
        return expediteur.substring(expediteur.indexOf('@') + 1);
    }
}
