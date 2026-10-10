package com.kupanga.api.email.event;

import com.kupanga.api.email.client.BrevoEmailClient;
import com.kupanga.api.email.dto.BrevoEmail;
import com.kupanga.api.email.dto.BrevoEmail.Attachment;
import com.kupanga.api.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * B11 : envoie les e-mails <b>après le commit</b> de la transaction qui les a demandés (rien n'est envoyé si elle
 * est annulée), dans l'exécuteur dédié {@code emailExecutor}. Sans transaction, l'envoi part tout de suite.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEnvoiListener {

    private final BrevoEmailClient brevoClient;
    private final MinioService minioService;

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void envoyer(EmailAEnvoyer evenement) {
        BrevoEmail email = evenement.email();
        List<Attachment> piecesJointes = piecesJointes(evenement.pdfs());
        if (!piecesJointes.isEmpty()) {
            email = new BrevoEmail(email.sender(), email.to(), email.subject(), email.htmlContent(), piecesJointes);
        }
        try {
            brevoClient.send(email);
        } catch (Exception e) {
            // Ni destinataire ni sujet (qui peut contenir le prénom) dans les logs
            log.warn("E-mail non envoyé : {}", e.getClass().getSimpleName());
        }
    }

    /** Lit chaque PDF dans son bucket privé et le joint en base64 (P0-7) ; un PDF illisible est omis. */
    private List<Attachment> piecesJointes(List<EmailAEnvoyer.PdfAJoindre> pdfs) {
        List<Attachment> piecesJointes = new ArrayList<>();
        for (EmailAEnvoyer.PdfAJoindre pdf : pdfs) {
            try {
                piecesJointes.add(new Attachment(
                        Base64.getEncoder().encodeToString(minioService.telecharger(pdf.bucket(), pdf.cle())),
                        pdf.nomFichier()));
            } catch (Exception e) {
                log.error("PDF {} illisible dans {}, e-mail envoyé sans pièce jointe : {}",
                        pdf.nomFichier(), pdf.bucket(), e.getClass().getSimpleName());
            }
        }
        return piecesJointes;
    }
}
