package com.kupanga.api.backoffice.dto;

import com.kupanga.api.email.file.EmailEnFile;
import com.kupanga.api.email.file.PrioriteEmail;
import com.kupanga.api.email.file.StatutEmail;
import com.kupanga.api.email.file.TypeEmail;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * M4 : ligne de la page « E-mails » du back-office. Ni contenu, ni sujet (prénom, liens avec jeton) ; adresse
 * masquée ({@code j***@gmail.com}).
 */
public record EmailAdminDTO(
        Long         id,
        LocalDateTime creeLe,
        TypeEmail    type,
        PrioriteEmail priorite,
        String       destinataireMasque,
        StatutEmail  statut,
        int          tentatives,
        LocalDateTime prochainEssai,
        LocalDateTime envoyeLe,
        String       derniereErreur,
        boolean      renvoyable
) {

    public static EmailAdminDTO from(EmailEnFile email, Instant maintenant) {
        return new EmailAdminDTO(
                email.getId(),
                utc(email.getCreeLe()),
                email.getType(),
                email.getPriorite(),
                masquer(email.getDestinataire()),
                email.getStatut(),
                email.getTentatives(),
                utc(email.getProchainEssai()),
                utc(email.getEnvoyeLe()),
                email.getDerniereErreur(),
                estRenvoyable(email, maintenant)
        );
    }

    /** Dates affichées en UTC (fuseau du serveur indifférent). */
    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /** Un e-mail en échec dont le contenu est encore là et dont le lien n'a pas expiré. */
    public static boolean estRenvoyable(EmailEnFile email, Instant maintenant) {
        return email.getStatut() == StatutEmail.ECHEC_DEFINITIF
                && !email.isContenuEfface()
                && email.getHtml() != null
                && (email.getExpireLe() == null || maintenant.isBefore(email.getExpireLe()));
    }

    /** {@code jean.dupont@gmail.com} → {@code j***@gmail.com} ; adresse sans « @ » → {@code ***}. */
    public static String masquer(String adresse) {
        if (adresse == null) return null;
        int arobase = adresse.indexOf('@');
        if (arobase <= 0) return "***";
        return adresse.charAt(0) + "***" + adresse.substring(arobase);
    }
}
