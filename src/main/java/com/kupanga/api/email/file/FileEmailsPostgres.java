package com.kupanga.api.email.file;

import com.kupanga.api.config.ApresCommit;
import com.kupanga.api.email.dto.EmailSortant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * M2 : file d'e-mails en base PostgreSQL (« transactional outbox »).
 * <ul>
 *     <li>la ligne est écrite <b>dans la transaction de l'appelant</b> (B11 conservé : rien n'est envoyé si elle est
 *     annulée) ; sans transaction, dans la sienne ;</li>
 *     <li>l'envoyeur est réveillé juste après le commit (envoi en quelques secondes) ; s'il ne l'est pas (plantage,
 *     redémarrage), sa tâche planifiée reprend la ligne au plus tard 30 s après.</li>
 * </ul>
 */
@Slf4j
@Component
public class FileEmailsPostgres implements FileEmails {

    private final EmailEnFileRepository depot;
    private final EnvoyeurEmails envoyeur;
    private final Clock horloge;

    @Autowired
    public FileEmailsPostgres(EmailEnFileRepository depot, @Lazy EnvoyeurEmails envoyeur) {
        this(depot, envoyeur, Clock.systemUTC());
    }

    FileEmailsPostgres(EmailEnFileRepository depot, EnvoyeurEmails envoyeur, Clock horloge) {
        this.depot = depot;
        this.envoyeur = envoyeur;
        this.horloge = horloge;
    }

    @Override
    @Transactional
    public void ajouter(EmailSortant email, TypeEmail type, List<PdfAJoindre> pdfs) {
        Instant maintenant = horloge.instant();
        EmailEnFile enFile = depot.save(EmailEnFile.builder()
                .creeLe(maintenant)
                .type(type)
                .priorite(type.getPriorite())
                .destinataire(email.destinataire())
                .sujet(tronquer(email.sujet(), 500))
                .html(email.html())
                .texte(email.texte())
                .piecesJointes(new ArrayList<>(pdfs == null ? List.of() : pdfs))
                .statut(StatutEmail.A_ENVOYER)
                .tentatives(0)
                .prochainEssai(maintenant)
                .expireLe(type.getValiditeLien() == null ? null : maintenant.plus(type.getValiditeLien()))
                .build());
        log.debug("E-mail {} ({}) mis en file", enFile.getId(), type);
        ApresCommit.executer(envoyeur::reveiller);
    }

    private static String tronquer(String valeur, int max) {
        return valeur == null || valeur.length() <= max ? valeur : valeur.substring(0, max);
    }
}
