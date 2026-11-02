package com.kupanga.api.email.file;

import com.kupanga.api.email.dto.EmailSortant;
import com.kupanga.api.email.dto.EmailSortant.PieceJointe;
import com.kupanga.api.email.envoi.EchecEnvoiEmail;
import com.kupanga.api.email.envoi.ExpediteurEmail;
import com.kupanga.api.stockage.service.StockageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * M2 : envoie les e-mails de la file {@code emails_sortants}.
 * <ul>
 *     <li>réveillé après le commit qui a mis un e-mail en file, et toutes les 30 s par une tâche planifiée (reprise
 *     après une panne du serveur SMTP, un plantage ou un redémarrage) ;</li>
 *     <li>réserve un lot ({@code FOR UPDATE SKIP LOCKED}, verrou de {@link #VERROU}), puis envoie <b>hors
 *     transaction</b> (aucune connexion à la base tenue pendant l'appel SMTP) ;</li>
 *     <li>erreur passagère : nouvel essai après {@link #DELAIS} (1 min … 24 h, environ 46 h en tout) ; refus définitif
 *     (5xx) ou essais épuisés : {@code ECHEC_DEFINITIF}, alerte (log ERROR → Sentry) si l'e-mail est critique ;
 *     lien expiré avant l'envoi : {@code PERIME} ;</li>
 *     <li>contenu effacé dès l'envoi (liens de connexion, de reset, de signature) ; purge quotidienne (RGPD).</li>
 * </ul>
 * Garantie « au moins une fois » : si l'application s'arrête entre l'acceptation par le serveur SMTP et
 * l'enregistrement de l'envoi, l'e-mail repart une fois à l'expiration du verrou.
 */
@Slf4j
@Component
public class EnvoyeurEmails {

    /** Délai avant chaque nouvel essai, après le 1er, 2e… échec passager. */
    static final List<Duration> DELAIS = List.of(
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofHours(1),
            Duration.ofHours(3), Duration.ofHours(6), Duration.ofHours(12), Duration.ofHours(24));

    /** Au-delà, un e-mail réservé par un envoyeur arrêté est repris (lot de {@link #LOT} × ~50 s d'envoi au plus). */
    static final Duration VERROU = Duration.ofMinutes(15);
    static final int LOT = 10;
    /** Lots traités par passage, pour rendre la main régulièrement. */
    static final int LOTS_MAX = 20;

    static final Duration CONSERVATION_CONTENU = Duration.ofDays(30);
    static final Duration CONSERVATION_LIGNES = Duration.ofDays(90);
    static final List<StatutEmail> TERMINES = List.of(StatutEmail.ENVOYE, StatutEmail.ECHEC_DEFINITIF, StatutEmail.PERIME);

    private final EmailEnFileRepository depot;
    private final ExpediteurEmail expediteur;
    private final StockageService stockageService;
    private final TransactionTemplate transaction;
    private final Clock horloge;
    private final boolean actif;

    private final ReentrantLock enCours = new ReentrantLock();
    private final AtomicBoolean relancer = new AtomicBoolean();

    @Autowired
    public EnvoyeurEmails(EmailEnFileRepository depot,
                          ExpediteurEmail expediteur,
                          StockageService stockageService,
                          PlatformTransactionManager transactionManager,
                          @Value("${email.envoi.actif:true}") boolean actif) {
        this(depot, expediteur, stockageService, new TransactionTemplate(transactionManager), Clock.systemUTC(), actif);
    }

    EnvoyeurEmails(EmailEnFileRepository depot, ExpediteurEmail expediteur, StockageService stockageService,
                   TransactionTemplate transaction, Clock horloge, boolean actif) {
        this.depot = depot;
        this.expediteur = expediteur;
        this.stockageService = stockageService;
        this.transaction = transaction;
        this.horloge = horloge;
        this.actif = actif;
    }

    /** Appelé après le commit qui a mis un e-mail en file : envoi en quelques secondes, dans un autre thread. */
    @Async("emailExecutor")
    public void reveiller() {
        if (actif) {
            traiter();
        }
    }

    /**
     * Reprise : e-mails dont le nouvel essai est dû, ou réservés par un envoyeur arrêté.
     * Durées en ISO-8601 ({@code PT30S}, {@code PT5M}) ou en millisecondes : Spring 6.1 refuse « 30s »
     * (format court seulement depuis Spring 6.2) et l'application ne démarre plus.
     */
    @Scheduled(fixedDelayString = "${email.envoi.intervalle:PT30S}", initialDelayString = "${email.envoi.delai-initial:PT20S}")
    public void reprendre() {
        if (actif) {
            traiter();
        }
    }

    /**
     * Envoie tout ce qui est dû. Un seul passage à la fois par instance ; un réveil pendant un passage en provoque
     * en général un nouveau juste après (au pire, l'e-mail attend la tâche planifiée, 30 s).
     *
     * @return nombre d'e-mails traités (envoyés ou non)
     */
    public int traiter() {
        if (!enCours.tryLock()) {
            relancer.set(true);
            return 0;
        }
        int traites = 0;
        try {
            do {
                relancer.set(false);
                traites += traiterLots();
            } while (relancer.get());
        } catch (RuntimeException e) {
            // Base indisponible… : la tâche planifiée réessaiera
            log.warn("File d'e-mails non traitée : {}", e.getClass().getSimpleName());
        } finally {
            enCours.unlock();
        }
        return traites;
    }

    private int traiterLots() {
        int traites = 0;
        for (int i = 0; i < LOTS_MAX; i++) {
            Instant maintenant = horloge.instant();
            List<Long> ids = transaction.execute(s -> depot.reserver(maintenant, maintenant.plus(VERROU), LOT));
            if (ids == null || ids.isEmpty()) {
                break;
            }
            for (Long id : ids) {
                envoyerUn(id);
                traites++;
            }
        }
        return traites;
    }

    void envoyerUn(Long id) {
        EmailEnFile email = depot.findById(id).orElse(null);
        if (email == null || email.getStatut() != StatutEmail.EN_COURS) {
            return;
        }
        if (email.getExpireLe() != null && horloge.instant().isAfter(email.getExpireLe())) {
            mettreAJour(id, e -> {
                terminer(e, StatutEmail.PERIME, "Lien expiré avant l'envoi");
                effacerContenu(e); // inutile désormais (renvoi refusé) et contient un jeton
            });
            if (email.getPriorite() == PrioriteEmail.CRITIQUE) {
                log.error("E-mail critique {} ({}) jamais distribué : lien expiré avant l'envoi", id, email.getType());
            } else {
                log.info("E-mail {} ({}) périmé avant l'envoi", id, email.getType());
            }
            return;
        }
        try {
            expediteur.envoyer(new EmailSortant(email.getDestinataire(), email.getSujet(), email.getHtml(),
                    email.getTexte(), piecesJointes(email)));
            mettreAJour(id, this::envoye);
        } catch (EchecEnvoiEmail e) {
            mettreAJour(id, enFile -> echec(enFile, e.isDefinitif(), e.getMessage()));
        } catch (RuntimeException e) {
            mettreAJour(id, enFile -> echec(enFile, false, "Erreur inattendue : " + e.getClass().getSimpleName()));
        }
    }

    private void mettreAJour(Long id, Consumer<EmailEnFile> modification) {
        transaction.executeWithoutResult(s -> depot.findById(id).ifPresent(e -> {
            modification.accept(e);
            depot.save(e);
        }));
    }

    private void envoye(EmailEnFile email) {
        email.setStatut(StatutEmail.ENVOYE);
        email.setEnvoyeLe(horloge.instant());
        email.setVerrouJusqua(null);
        email.setDerniereErreur(null);
        effacerContenu(email);
        log.debug("E-mail {} ({}) envoyé", email.getId(), email.getType());
    }

    private void echec(EmailEnFile email, boolean definitif, String erreur) {
        email.setTentatives(email.getTentatives() + 1);
        email.setDerniereErreur(tronquer(erreur));
        email.setVerrouJusqua(null);
        if (definitif || email.getTentatives() > DELAIS.size()) {
            terminer(email, StatutEmail.ECHEC_DEFINITIF, erreur);
            if (email.getPriorite() == PrioriteEmail.CRITIQUE) {
                // Sentry : ni adresse, ni sujet, ni contenu (W10)
                log.error("E-mail critique {} ({}) non distribué après {} tentative(s) : {}",
                        email.getId(), email.getType(), email.getTentatives(), tronquer(erreur));
            } else {
                log.warn("E-mail {} ({}) non distribué : {}", email.getId(), email.getType(), tronquer(erreur));
            }
            return;
        }
        email.setStatut(StatutEmail.A_ENVOYER);
        email.setProchainEssai(horloge.instant().plus(DELAIS.get(email.getTentatives() - 1)));
        log.warn("E-mail {} ({}) non envoyé, essai {} : {}", email.getId(), email.getType(),
                email.getTentatives(), tronquer(erreur));
    }

    private void terminer(EmailEnFile email, StatutEmail statut, String erreur) {
        email.setStatut(statut);
        email.setVerrouJusqua(null);
        email.setDerniereErreur(tronquer(erreur));
    }

    private static void effacerContenu(EmailEnFile email) {
        email.setHtml(null);
        email.setTexte(null);
        email.setContenuEfface(true);
    }

    /** P0-7 : PDF lus dans leur bucket privé au moment de l'envoi ; un PDF illisible est omis (e-mail envoyé quand même). */
    private List<PieceJointe> piecesJointes(EmailEnFile email) {
        List<PieceJointe> pieces = new ArrayList<>();
        for (PdfAJoindre pdf : email.getPiecesJointes()) {
            try {
                pieces.add(new PieceJointe(pdf.nomFichier(), stockageService.telecharger(pdf.bucket(), pdf.cle()),
                        "application/pdf"));
            } catch (RuntimeException e) {
                log.error("PDF illisible pour l'e-mail {}, envoyé sans pièce jointe : {}",
                        email.getId(), e.getClass().getSimpleName());
            }
        }
        return pieces;
    }

    /**
     * RGPD : contenu des e-mails non envoyés effacé après {@link #CONSERVATION_CONTENU}, ou dès l'expiration du lien
     * qu'il contient (jeton) ; lignes terminées supprimées après {@link #CONSERVATION_LIGNES}.
     */
    @Scheduled(cron = "${email.purge.cron:0 15 4 * * *}")
    public void purger() {
        if (!actif) {
            return;
        }
        Instant maintenant = horloge.instant();
        List<StatutEmail> nonEnvoyes = List.of(StatutEmail.ECHEC_DEFINITIF, StatutEmail.PERIME);
        Integer effaces = transaction.execute(s -> depot.effacerContenu(maintenant.minus(CONSERVATION_CONTENU), nonEnvoyes)
                + depot.effacerContenuExpire(maintenant, nonEnvoyes));
        List<Long> anciens = depot.terminesAvant(maintenant.minus(CONSERVATION_LIGNES), TERMINES);
        transaction.executeWithoutResult(s -> depot.deleteAllById(anciens));
        log.info("Purge de la file d'e-mails : {} contenu(s) effacé(s), {} ligne(s) supprimée(s)", effaces, anciens.size());
    }

    private static String tronquer(String erreur) {
        if (erreur == null) return null;
        return erreur.length() <= 200 ? erreur : erreur.substring(0, 200);
    }
}
