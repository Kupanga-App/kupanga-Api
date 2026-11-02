package com.kupanga.api.email.file;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kupanga.api.email.dto.EmailSortant;
import com.kupanga.api.email.envoi.EchecEnvoiEmail;
import com.kupanga.api.email.envoi.ExpediteurEmail;
import com.kupanga.api.stockage.service.StockageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M2 sur base réelle (sans transaction de test : chaque étape est validée, comme en production).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Tests d'intégration — file d'envoi persistante des e-mails (M2)")
class FileEmailsIntegrationTest {

    @Autowired private EmailEnFileRepository depot;
    @Autowired private PlatformTransactionManager transactionManager;

    private final AtomicReference<Instant> maintenant = new AtomicReference<>(Instant.parse("2026-11-02T10:00:00Z"));
    private final Clock horloge = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant.get(); }
    };

    private TransactionTemplate transaction;
    private ExpediteurEmail expediteur;
    private StockageService stockage;
    private EnvoyeurEmails envoyeurReveille;
    private FileEmailsPostgres file;
    private EnvoyeurEmails envoyeur;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void setUp() {
        depot.deleteAll();
        transaction = new TransactionTemplate(transactionManager);
        expediteur = mock(ExpediteurEmail.class);
        stockage = mock(StockageService.class);
        envoyeurReveille = mock(EnvoyeurEmails.class);
        file = new FileEmailsPostgres(depot, envoyeurReveille, horloge);
        envoyeur = new EnvoyeurEmails(depot, expediteur, stockage, transaction, horloge, true);
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(EnvoyeurEmails.class)).addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(EnvoyeurEmails.class)).detachAppender(logs);
        depot.deleteAll();
    }

    private void mettreEnFile(TypeEmail type, String destinataire) {
        transaction.executeWithoutResult(s -> file.ajouter(
                EmailSortant.html(destinataire, "Sujet", "<p>Lien : https://kupanga.test/x?token=SECRET</p>"),
                type, List.of()));
    }

    private EmailEnFile seul() {
        List<EmailEnFile> tous = depot.findAll();
        assertThat(tous).hasSize(1);
        return tous.get(0);
    }

    @Test
    @DisplayName("B11 : transaction annulée → aucun e-mail en file ni envoyé ; validée → en file, envoyeur réveillé après le commit")
    void transactionAnnuleeOuValidee() {
        transaction.executeWithoutResult(s -> {
            file.ajouter(EmailSortant.html("a@exemple.fr", "S", "<p>x</p>"), TypeEmail.BIENVENUE, List.of());
            s.setRollbackOnly();
        });
        assertThat(depot.count()).isZero();
        verifyNoInteractions(envoyeurReveille);

        mettreEnFile(TypeEmail.RESET_MOT_DE_PASSE, "b@exemple.fr");

        EmailEnFile email = seul();
        assertThat(email.getStatut()).isEqualTo(StatutEmail.A_ENVOYER);
        assertThat(email.getPriorite()).isEqualTo(PrioriteEmail.CRITIQUE);
        assertThat(email.getExpireLe()).isEqualTo(maintenant.get().plus(Duration.ofMinutes(10)));
        verify(envoyeurReveille).reveiller();
    }

    @Test
    @DisplayName("Envoi réussi → ENVOYE, contenu (lien avec jeton) effacé")
    void envoiReussi() throws Exception {
        mettreEnFile(TypeEmail.VERIFICATION_EMAIL, "a@exemple.fr");

        assertThat(envoyeur.traiter()).isEqualTo(1);

        ArgumentCaptor<EmailSortant> envoye = ArgumentCaptor.forClass(EmailSortant.class);
        verify(expediteur).envoyer(envoye.capture());
        assertThat(envoye.getValue().destinataire()).isEqualTo("a@exemple.fr");
        assertThat(envoye.getValue().texte()).contains("token=SECRET");
        EmailEnFile email = seul();
        assertThat(email.getStatut()).isEqualTo(StatutEmail.ENVOYE);
        assertThat(email.getEnvoyeLe()).isEqualTo(maintenant.get());
        assertThat(email.getHtml()).isNull();
        assertThat(email.getTexte()).isNull();
        assertThat(email.isContenuEfface()).isTrue();
    }

    @Test
    @DisplayName("Serveur SMTP en panne → nouvel essai 1 min plus tard, pas avant ; envoyé quand le serveur revient")
    void pannePuisRetour() throws Exception {
        mettreEnFile(TypeEmail.QUITTANCE, "a@exemple.fr");
        doThrow(new EchecEnvoiEmail("Serveur SMTP injoignable : ConnectException", false, null, null))
                .doNothing()
                .when(expediteur).envoyer(any());

        envoyeur.traiter();
        EmailEnFile apresPanne = seul();
        assertThat(apresPanne.getStatut()).isEqualTo(StatutEmail.A_ENVOYER);
        assertThat(apresPanne.getTentatives()).isEqualTo(1);
        assertThat(apresPanne.getProchainEssai()).isEqualTo(maintenant.get().plus(Duration.ofMinutes(1)));
        assertThat(apresPanne.getDerniereErreur()).isEqualTo("Serveur SMTP injoignable : ConnectException");
        assertThat(apresPanne.getHtml()).isNotNull();

        envoyeur.traiter(); // trop tôt
        verify(expediteur, times(1)).envoyer(any());

        maintenant.set(maintenant.get().plusSeconds(61));
        envoyeur.traiter();
        verify(expediteur, times(2)).envoyer(any());
        assertThat(seul().getStatut()).isEqualTo(StatutEmail.ENVOYE);
    }

    @Test
    @DisplayName("Panne prolongée : délais croissants, puis échec définitif après le 9e essai et alerte (e-mail critique)")
    void essaisEpuises() throws Exception {
        mettreEnFile(TypeEmail.CONTRAT_SIGNE, "a@exemple.fr");
        doThrow(new EchecEnvoiEmail("Refus SMTP 451", false, 451, null)).when(expediteur).envoyer(any());

        for (int essai = 1; essai <= EnvoyeurEmails.DELAIS.size(); essai++) {
            envoyeur.traiter();
            EmailEnFile email = seul();
            assertThat(email.getStatut()).isEqualTo(StatutEmail.A_ENVOYER);
            assertThat(email.getProchainEssai()).isEqualTo(maintenant.get().plus(EnvoyeurEmails.DELAIS.get(essai - 1)));
            maintenant.set(email.getProchainEssai());
        }
        envoyeur.traiter();

        EmailEnFile email = seul();
        assertThat(email.getStatut()).isEqualTo(StatutEmail.ECHEC_DEFINITIF);
        assertThat(email.getTentatives()).isEqualTo(EnvoyeurEmails.DELAIS.size() + 1);
        assertThat(email.getHtml()).isNotNull(); // gardé pour un renvoi depuis le back-office
        assertThat(logs.list).anyMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("E-mail critique")
                && !e.getFormattedMessage().contains("a@exemple.fr"));
    }

    @Test
    @DisplayName("Refus définitif (550) → ECHEC_DEFINITIF tout de suite, sans nouvel essai")
    void refusDefinitif() throws Exception {
        mettreEnFile(TypeEmail.BIENVENUE, "inconnu@exemple.fr");
        doThrow(new EchecEnvoiEmail("Refus SMTP 550", true, 550, null)).when(expediteur).envoyer(any());

        envoyeur.traiter();
        maintenant.set(maintenant.get().plus(Duration.ofDays(3)));
        envoyeur.traiter();

        verify(expediteur, times(1)).envoyer(any());
        assertThat(seul().getStatut()).isEqualTo(StatutEmail.ECHEC_DEFINITIF);
        // e-mail non critique : pas d'alerte ERROR
        assertThat(logs.list).noneMatch(e -> e.getLevel() == Level.ERROR);
    }

    @Test
    @DisplayName("Lien de reset expiré avant le retour du serveur → PERIME, jamais envoyé")
    void lienPerime() throws Exception {
        mettreEnFile(TypeEmail.RESET_MOT_DE_PASSE, "a@exemple.fr");
        maintenant.set(maintenant.get().plus(Duration.ofMinutes(11)));

        envoyeur.traiter();

        verify(expediteur, never()).envoyer(any());
        EmailEnFile email = seul();
        assertThat(email.getStatut()).isEqualTo(StatutEmail.PERIME);
        // Revue M2 : contenu (jeton) effacé tout de suite ; alerte, l'e-mail était critique
        assertThat(email.getHtml()).isNull();
        assertThat(email.isContenuEfface()).isTrue();
        assertThat(logs.list).anyMatch(e -> e.getLevel() == Level.ERROR
                && e.getFormattedMessage().contains("jamais distribué"));
    }

    @Test
    @DisplayName("Revue M2 : échec définitif d'un e-mail à lien → contenu effacé dès l'expiration du lien")
    void echecDefinitif_contenuEffaceAExpiration() throws Exception {
        mettreEnFile(TypeEmail.VERIFICATION_EMAIL, "a@exemple.fr");
        doThrow(new EchecEnvoiEmail("Destinataire refusé, SMTP 550", true, 550, null)).when(expediteur).envoyer(any());
        envoyeur.traiter();

        envoyeur.purger();
        assertThat(seul().getHtml()).isNotNull(); // lien encore valable : renvoi possible

        maintenant.set(maintenant.get().plus(Duration.ofHours(25)));
        envoyeur.purger();
        assertThat(seul().getHtml()).isNull();
        assertThat(seul().isContenuEfface()).isTrue();
    }

    @Test
    @DisplayName("Envoyeur arrêté en plein envoi (verrou expiré) → e-mail repris ; verrou encore valide → laissé")
    void repriseApresPlantage() throws Exception {
        mettreEnFile(TypeEmail.QUITTANCE, "a@exemple.fr");
        EmailEnFile email = seul();
        email.setStatut(StatutEmail.EN_COURS);
        email.setVerrouJusqua(maintenant.get().plus(Duration.ofMinutes(5)));
        depot.save(email);

        envoyeur.traiter();
        verify(expediteur, never()).envoyer(any());

        maintenant.set(maintenant.get().plus(Duration.ofMinutes(6)));
        envoyeur.traiter();
        verify(expediteur).envoyer(any());
        assertThat(seul().getStatut()).isEqualTo(StatutEmail.ENVOYE);
    }

    @Test
    @DisplayName("Deux envoyeurs en même temps (deux instances) : chaque e-mail part une seule fois (SKIP LOCKED)")
    void deuxEnvoyeursConcurrents() throws Exception {
        for (int i = 0; i < 30; i++) {
            mettreEnFile(TypeEmail.BIENVENUE, "a" + i + "@exemple.fr");
        }
        Map<String, AtomicInteger> envois = new ConcurrentHashMap<>();
        ExpediteurEmail lent = email -> {
            envois.computeIfAbsent(email.destinataire(), d -> new AtomicInteger()).incrementAndGet();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        EnvoyeurEmails instanceA = new EnvoyeurEmails(depot, lent, stockage, transaction, horloge, true);
        EnvoyeurEmails instanceB = new EnvoyeurEmails(depot, lent, stockage, transaction, horloge, true);

        ExecutorService threads = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);
        threads.submit(() -> { depart.await(); return instanceA.traiter(); });
        threads.submit(() -> { depart.await(); return instanceB.traiter(); });
        depart.countDown();
        threads.shutdown();
        assertThat(threads.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(envois).hasSize(30);
        assertThat(envois.values()).allMatch(n -> n.get() == 1);
        assertThat(depot.findAll()).allMatch(e -> e.getStatut() == StatutEmail.ENVOYE);
    }

    @Test
    @DisplayName("PDF joint lu dans le stockage au moment de l'envoi ; PDF illisible → e-mail envoyé sans pièce jointe")
    void piecesJointes() throws Exception {
        transaction.executeWithoutResult(s -> file.ajouter(EmailSortant.html("a@exemple.fr", "Q", "<p>q</p>"),
                TypeEmail.QUITTANCE, List.of(new PdfAJoindre("bucket-des-quittances", "q.pdf", "Quittance.pdf"))));
        transaction.executeWithoutResult(s -> file.ajouter(EmailSortant.html("b@exemple.fr", "Q", "<p>q</p>"),
                TypeEmail.QUITTANCE, List.of(new PdfAJoindre("bucket-des-quittances", "illisible.pdf", "Q2.pdf"))));
        when(stockage.telecharger("bucket-des-quittances", "q.pdf")).thenReturn("%PDF".getBytes());
        when(stockage.telecharger("bucket-des-quittances", "illisible.pdf")).thenThrow(new IllegalStateException("absent"));

        envoyeur.traiter();

        ArgumentCaptor<EmailSortant> envoyes = ArgumentCaptor.forClass(EmailSortant.class);
        verify(expediteur, times(2)).envoyer(envoyes.capture());
        EmailSortant avec = envoyes.getAllValues().stream().filter(e -> e.destinataire().equals("a@exemple.fr")).findFirst().orElseThrow();
        EmailSortant sans = envoyes.getAllValues().stream().filter(e -> e.destinataire().equals("b@exemple.fr")).findFirst().orElseThrow();
        assertThat(avec.piecesJointes()).singleElement()
                .satisfies(p -> {
                    assertThat(p.nomFichier()).isEqualTo("Quittance.pdf");
                    assertThat(p.contenu()).isEqualTo("%PDF".getBytes());
                });
        assertThat(sans.piecesJointes()).isEmpty();
    }

    @Test
    @DisplayName("RGPD : contenu des échecs effacé après 30 jours, lignes terminées supprimées après 90 jours")
    void purge() throws Exception {
        mettreEnFile(TypeEmail.BIENVENUE, "ancien@exemple.fr");
        doThrow(new EchecEnvoiEmail("Refus SMTP 550", true, 550, null)).when(expediteur).envoyer(any());
        envoyeur.traiter();

        maintenant.set(maintenant.get().plus(Duration.ofDays(31)));
        envoyeur.purger();
        EmailEnFile echec = seul();
        assertThat(echec.getHtml()).isNull();
        assertThat(echec.isContenuEfface()).isTrue();

        mettreEnFile(TypeEmail.BIENVENUE, "recent@exemple.fr"); // en attente : jamais purgé
        maintenant.set(maintenant.get().plus(Duration.ofDays(60)));
        envoyeur.purger();
        assertThat(depot.findAll()).extracting(EmailEnFile::getDestinataire).containsExactly("recent@exemple.fr");
    }
}
