package com.kupanga.api.email.file;

import com.kupanga.api.email.dto.EmailSortant;

import java.util.List;

/**
 * M2 / M3 : met un e-mail en file d'envoi. L'implémentation actuelle ({@link FileEmailsPostgres}) l'écrit en base
 * dans la transaction de l'appelant (décision du 2026-11-02 : outbox seule, pas de broker) ; une implémentation
 * RabbitMQ pourrait publier depuis cette même table le jour où l'API tournera sur plusieurs instances.
 */
public interface FileEmails {

    /**
     * @param email e-mail préparé (sans pièce jointe : les PDF sont lus au moment de l'envoi)
     * @param type  nature de l'e-mail (priorité, validité du lien)
     * @param pdfs  PDF à joindre, par leur clé dans un bucket privé
     */
    void ajouter(EmailSortant email, TypeEmail type, List<PdfAJoindre> pdfs);
}
