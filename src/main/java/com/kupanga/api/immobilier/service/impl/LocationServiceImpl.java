package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.LocationDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.Location;
import com.kupanga.api.immobilier.entity.PartieLocation;
import com.kupanga.api.immobilier.entity.StatutLocation;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.LocationRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.LocationService;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.utils.EmailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class LocationServiceImpl implements LocationService {

    private static final java.time.format.DateTimeFormatter FORMAT_JOUR =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LocationRepository locationRepository;
    private final BienRepository bienRepository;
    private final ContratRepository contratRepository;
    private final BienService bienService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final com.kupanga.api.juridiction.JuridictionRegistry juridictionRegistry;
    private final TransactionTemplate transactionParReservation;
    private final Clock clock;

    @Autowired
    public LocationServiceImpl(LocationRepository locationRepository, BienRepository bienRepository,
                               ContratRepository contratRepository, BienService bienService,
                               NotificationService notificationService, EmailService emailService,
                               com.kupanga.api.juridiction.JuridictionRegistry juridictionRegistry,
                               PlatformTransactionManager transactions) {
        this(locationRepository, bienRepository, contratRepository, bienService, notificationService, emailService,
                juridictionRegistry, new TransactionTemplate(transactions), Clock.systemDefaultZone());
    }

    /** Horloge fixée par les tests (échéance des réservations). */
    LocationServiceImpl(LocationRepository locationRepository, BienRepository bienRepository,
                        ContratRepository contratRepository, BienService bienService,
                        NotificationService notificationService, EmailService emailService,
                        com.kupanga.api.juridiction.JuridictionRegistry juridictionRegistry,
                        TransactionTemplate transactionParReservation, Clock clock) {
        this.locationRepository = locationRepository;
        this.bienRepository = bienRepository;
        this.contratRepository = contratRepository;
        this.bienService = bienService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.juridictionRegistry = juridictionRegistry;
        this.transactionParReservation = transactionParReservation;
        this.clock = clock;
    }

    // ─── Usage interne ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void verifierBienLibre(Bien bien, LocalDateTime maintenant) {
        // Revue lot 2 : expiration complète (baux non signés annulés, parties prévenues), pas seulement le statut :
        // sinon l'ancien bail redeviendrait signable si le même candidat réservait de nouveau
        for (Long id : locationRepository.findIdsReservationsEchuesDuBien(bien.getId(), maintenant)) {
            expirer(id);
        }
        if (bien.getLocataire() != null) {
            throw new KupangaBusinessException(
                    "Ce bien a déjà un locataire : libérez-le avant de le proposer à quelqu'un d'autre",
                    HttpStatus.CONFLICT);
        }
        if (locationRepository.findReservationValable(bien.getId(), maintenant).isPresent()) {
            throw new KupangaBusinessException(
                    "Ce bien est réservé : attendez la signature du bail ou retirez la réservation",
                    HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public Location reserver(DemandeLocation demande, LocalDateTime maintenant) {
        Location location = Location.builder()
                .bien(demande.getBien())
                .proprietaire(demande.getProprietaire())
                .locataire(demande.getLocataire())
                .demande(demande)
                .statut(StatutLocation.RESERVEE)
                .dateReservation(maintenant)
                .dateExpiration(maintenant.plus(DUREE_RESERVATION))
                .build();
        try {
            return locationRepository.saveAndFlush(location);
        } catch (DataIntegrityViolationException e) {
            // Réservation ou location déjà en cours sur ce bien (index unique partiel, V54)
            throw plusDisponible();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Location> reservationValable(Long bienId) {
        return locationRepository.findReservationValable(bienId, LocalDateTime.now(clock));
    }

    @Override
    @Transactional(readOnly = true)
    public User locataireDuBail(Bien bien, String emailLocataire) {
        String email = emailLocataire == null ? null : EmailUtils.normaliser(emailLocataire);
        if (email != null) {
            User actuel = bien.getLocataire();
            if (actuel != null && actuel.getMail().equals(email)) {
                return actuel;
            }
            Optional<User> candidat = reservationValable(bien.getId())
                    .map(Location::getLocataire)
                    .filter(l -> l.getMail().equals(email));
            if (candidat.isPresent()) {
                return candidat.get();
            }
        }
        throw new KupangaBusinessException(
                "Cette personne n'est ni le locataire de ce bien ni le candidat qui l'a réservé",
                HttpStatus.BAD_REQUEST);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifierBailSignable(Contrat contrat) {
        if (estLocataireActuel(contrat)) {
            return;
        }
        boolean reserve = reservationValable(contrat.getBien().getId())
                .filter(l -> l.getLocataire().getId().equals(contrat.getLocataire().getId()))
                .isPresent();
        if (!reserve) {
            throw new KupangaBusinessException(
                    "La réservation de ce logement a pris fin : ce bail ne peut plus être signé",
                    HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public void activerApresSignature(Contrat contrat) {
        if (estLocataireActuel(contrat)) {
            return;
        }
        LocalDateTime maintenant = LocalDateTime.now(clock);
        Location location = locationRepository.findReservationValable(contrat.getBien().getId(), maintenant)
                .filter(l -> l.getLocataire().getId().equals(contrat.getLocataire().getId()))
                .orElseThrow(() -> new KupangaBusinessException(
                        "La réservation de ce logement a pris fin : ce bail ne peut plus être signé",
                        HttpStatus.CONFLICT));
        location.setStatut(StatutLocation.ACTIVE);
        location.setDateDebut(maintenant);
        // Verrou optimiste : un désistement ou un retrait simultané fait échouer la signature (409)
        locationRepository.saveAndFlush(location);

        Bien bien = contrat.getBien();
        bien.setLocataire(contrat.getLocataire());
        bienRepository.save(bien);

        notificationService.saveAndSend(
                contrat.getLocataire(),
                NotificationType.BIEN_ASSIGNE,
                "Bienvenue dans votre logement",
                "Le bail est signé : vous êtes maintenant locataire du logement situé au "
                        + bien.adresseComplete() + ".",
                null,
                bien.getId());
        log.info("Location {} active : bail {} signé sur le bien {}", location.getId(), contrat.getId(), bien.getId());
    }

    @Override
    public int expirerReservationsEchues() {
        List<Long> ids = locationRepository.findIdsReservationsEchues(LocalDateTime.now(clock));
        int expirees = 0;
        for (Long id : ids) {
            try {
                Boolean faite = transactionParReservation.execute(etat -> expirer(id));
                if (Boolean.TRUE.equals(faite)) {
                    expirees++;
                }
            } catch (RuntimeException e) {
                // Une réservation signée ou retirée au même moment : elle n'est plus à expirer
                log.warn("Expiration de la réservation {} impossible ({})", id, e.getClass().getSimpleName());
            }
        }
        if (expirees > 0) {
            log.info("{} réservation(s) expirée(s)", expirees);
        }
        return expirees;
    }

    private boolean expirer(Long id) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        Location location = locationRepository.findAvecParties(id).orElse(null);
        if (location == null || location.getStatut() != StatutLocation.RESERVEE
                || location.statutA(maintenant) != StatutLocation.EXPIREE) {
            return false;
        }
        terminer(location, StatutLocation.EXPIREE, location.getDateExpiration());
        Bien bien = location.getBien();
        notificationService.saveAndSend(
                location.getProprietaire(),
                NotificationType.RESERVATION_EXPIREE,
                "Réservation expirée",
                "Le bail du logement situé au " + bien.adresseComplete() + " n'a pas été signé à temps : "
                        + "la réservation de " + nom(location.getLocataire())
                        + " a pris fin et le logement est de nouveau en ligne.",
                null,
                bien.getId());
        notificationService.saveAndSend(
                location.getLocataire(),
                NotificationType.RESERVATION_EXPIREE_CANDIDAT,
                "Réservation expirée",
                "Votre réservation du logement situé au " + bien.adresseComplete()
                        + " a pris fin : le bail n'a pas été signé dans les 14 jours.",
                null,
                location.getId());
        emailService.envoyerFinReservation(location, StatutLocation.EXPIREE, location.getProprietaire());
        emailService.envoyerFinReservation(location, StatutLocation.EXPIREE, location.getLocataire());
        return true;
    }

    // ─── Routes ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<LocationDTO> enCoursDuBien(String emailProprietaire, Long bienId) {
        bienService.verifierProprietaire(bienId, emailProprietaire);
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return locationRepository.findReservationValable(bienId, maintenant)
                .or(() -> locationRepository.findActive(bienId))
                .map(l -> versDTO(l, maintenant));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDTO> mesReservations(String email) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return locationRepository.findReservationsDuLocataire(email, maintenant).stream()
                .map(l -> versDTO(l, maintenant)).toList();
    }

    @Override
    @Transactional
    public LocationDTO desister(String email, Long locationId) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        Location location = locationRepository.findAvecParties(locationId)
                .filter(l -> l.getLocataire().getMail().equals(email))
                .orElseThrow(LocationServiceImpl::introuvable);
        verifierReservee(location, maintenant);

        terminer(location, StatutLocation.DESISTEE, maintenant);
        Bien bien = location.getBien();
        notificationService.saveAndSend(
                location.getProprietaire(),
                NotificationType.RESERVATION_DESISTEE,
                "Désistement du candidat",
                nom(location.getLocataire()) + " s'est désisté pour le logement situé au "
                        + bien.adresseComplete() + ". Le logement est de nouveau en ligne : vous pouvez le "
                        + "proposer à une autre personne.",
                null,
                bien.getId());
        emailService.envoyerFinReservation(location, StatutLocation.DESISTEE, location.getProprietaire());
        log.info("Réservation {} : désistement du candidat", locationId);
        return versDTO(location, maintenant);
    }

    @Override
    @Transactional
    public LocationDTO annuler(String emailProprietaire, Long locationId) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        Location location = locationRepository.findAvecParties(locationId)
                .orElseThrow(LocationServiceImpl::introuvable);
        // Revue lot 2 : réservation d'un autre propriétaire → même 404 qu'un id inconnu (ids séquentiels)
        if (!location.getProprietaire().getMail().equals(emailProprietaire)) {
            throw introuvable();
        }
        verifierReservee(location, maintenant);

        terminer(location, StatutLocation.ANNULEE, maintenant);
        Bien bien = location.getBien();
        notificationService.saveAndSend(
                location.getLocataire(),
                NotificationType.RESERVATION_ANNULEE,
                "Réservation retirée",
                nom(location.getProprietaire()) + " a retiré votre réservation du logement situé au "
                        + bien.adresseComplete() + ".",
                null,
                location.getId());
        emailService.envoyerFinReservation(location, StatutLocation.ANNULEE, location.getLocataire());
        log.info("Réservation {} retirée par le propriétaire", locationId);
        return versDTO(location, maintenant);
    }

    // ─── Fin de bail ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LocationDTO donnerConge(String email, Long locationId, java.time.LocalDate dateFin) {
        Location location = locationActiveDe(email, locationId);
        // B12 : bien archivé ou partie anonymisée → lecture seule (409)
        bienService.verifierDocumentModifiable(location.getBien(), location.getProprietaire(), location.getLocataire());
        if (location.getDateFinPrevue() != null) {
            throw new KupangaBusinessException(
                    "Un congé est déjà donné pour le " + location.getDateFinPrevue().format(FORMAT_JOUR)
                            + " : retirez-le pour changer la date", HttpStatus.CONFLICT);
        }
        java.time.LocalDate aujourdhui = aujourdhui(location);
        if (dateFin.isBefore(aujourdhui) || dateFin.isAfter(aujourdhui.plus(CONGE_MAXIMAL))) {
            throw new KupangaBusinessException(
                    "La date de fin doit être comprise entre aujourd'hui et dans deux ans", HttpStatus.BAD_REQUEST);
        }
        boolean parProprietaire = location.getProprietaire().getMail().equals(email);
        location.setDateFinPrevue(dateFin);
        location.setCongeDonnePar(parProprietaire ? PartieLocation.PROPRIETAIRE : PartieLocation.LOCATAIRE);
        location.setDateConge(LocalDateTime.now(clock));
        locationRepository.saveAndFlush(location);

        User autre = parProprietaire ? location.getLocataire() : location.getProprietaire();
        User auteur = parProprietaire ? location.getProprietaire() : location.getLocataire();
        Bien bien = location.getBien();
        notificationService.saveAndSend(
                autre,
                parProprietaire ? NotificationType.CONGE_POUR_LOCATAIRE : NotificationType.CONGE_POUR_PROPRIETAIRE,
                "Congé reçu",
                nom(auteur) + " a donné congé pour le logement situé au " + bien.adresseComplete()
                        + " : fin du bail le " + dateFin.format(FORMAT_JOUR) + ".",
                null,
                bien.getId());
        emailService.envoyerConge(location, autre, false);
        log.info("Location {} : congé donné ({}) pour le {}", locationId, location.getCongeDonnePar(), dateFin);
        return versDTO(location, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public LocationDTO retirerConge(String email, Long locationId) {
        Location location = locationActiveDe(email, locationId);
        bienService.verifierDocumentModifiable(location.getBien(), location.getProprietaire(), location.getLocataire());
        boolean parProprietaire = location.getProprietaire().getMail().equals(email);
        PartieLocation moi = parProprietaire ? PartieLocation.PROPRIETAIRE : PartieLocation.LOCATAIRE;
        if (location.getCongeDonnePar() == null) {
            throw new KupangaBusinessException("Aucun congé n'est en cours", HttpStatus.CONFLICT);
        }
        if (location.getCongeDonnePar() != moi) {
            throw new KupangaBusinessException(
                    "Seule la personne qui a donné congé peut le retirer", HttpStatus.CONFLICT);
        }
        java.time.LocalDate ancienneDate = location.getDateFinPrevue();
        location.setDateFinPrevue(null);
        location.setCongeDonnePar(null);
        location.setDateConge(null);
        locationRepository.saveAndFlush(location);

        User autre = parProprietaire ? location.getLocataire() : location.getProprietaire();
        User auteur = parProprietaire ? location.getProprietaire() : location.getLocataire();
        Bien bien = location.getBien();
        notificationService.saveAndSend(
                autre,
                parProprietaire ? NotificationType.CONGE_POUR_LOCATAIRE : NotificationType.CONGE_POUR_PROPRIETAIRE,
                "Congé retiré",
                nom(auteur) + " a retiré le congé du " + ancienneDate.format(FORMAT_JOUR)
                        + " pour le logement situé au " + bien.adresseComplete() + " : le bail continue.",
                null,
                bien.getId());
        emailService.envoyerConge(location, autre, true);
        log.info("Location {} : congé retiré", locationId);
        return versDTO(location, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public LocationDTO terminer(String emailProprietaire, Long locationId, java.math.BigDecimal depotRestitue,
                                String motifRetenue) {
        Location location = locationRepository.findAvecParties(locationId)
                .filter(l -> l.getProprietaire().getMail().equals(emailProprietaire))
                .orElseThrow(LocationServiceImpl::locationIntrouvable);
        if (location.getStatut() != StatutLocation.ACTIVE) {
            throw new KupangaBusinessException("Cette location n'est pas en cours", HttpStatus.CONFLICT);
        }
        if (location.getDateFinPrevue() == null) {
            throw new KupangaBusinessException(
                    "Donnez d'abord congé (ou attendez celui du locataire) avant d'enregistrer la fin du bail",
                    HttpStatus.CONFLICT);
        }
        // Bien archivé : la fin reste possible (elle libère le locataire) ; partie anonymisée : 409
        if (location.getLocataire().isAnonymise()) {
            throw new KupangaBusinessException(
                    "Une des parties a supprimé son compte : cette location ne peut plus être modifiée",
                    HttpStatus.CONFLICT);
        }
        if (location.getDateFinPrevue().isAfter(aujourdhui(location))) {
            throw new KupangaBusinessException(
                    "La fin du bail est prévue le " + location.getDateFinPrevue().format(FORMAT_JOUR)
                            + " : vous pourrez l'enregistrer à partir de cette date", HttpStatus.CONFLICT);
        }
        java.math.BigDecimal depot = depotDuBail(location);
        if (depot != null && depotRestitue.compareTo(depot) > 0) {
            throw new KupangaBusinessException(
                    "Le montant restitué ne peut pas dépasser le dépôt de garantie du bail", HttpStatus.BAD_REQUEST);
        }
        String motif = motifRetenue == null || motifRetenue.isBlank() ? null : motifRetenue.strip();
        boolean retenue = depot == null || depotRestitue.compareTo(depot) < 0;
        if (retenue && depot != null && motif == null) {
            throw new KupangaBusinessException(
                    "Indiquez le motif de la retenue sur le dépôt de garantie", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime maintenant = LocalDateTime.now(clock);
        Bien bien = location.getBien();
        User locataire = location.getLocataire();
        // Baux non signés du locataire annulés (même ordre d'écriture que la signature : bail puis location)
        contratRepository.annulerNonSignesDuCandidat(bien.getId(), locataire.getId());
        location.setStatut(StatutLocation.TERMINEE);
        location.setDateFin(maintenant);
        location.setDepotRestitue(depotRestitue);
        location.setMotifRetenue(retenue ? motif : null);
        location = locationRepository.saveAndFlush(location);
        Bien aLiberer = bienRepository.findById(bien.getId()).orElseThrow();
        aLiberer.setLocataire(null);
        bienRepository.save(aLiberer);

        notificationService.saveAndSend(
                locataire,
                NotificationType.LOCATION_TERMINEE,
                "Fin du bail",
                "Le bail du logement situé au " + bien.adresseComplete() + " est terminé. Vos documents (bail, "
                        + "états des lieux, quittances) restent dans Kupanga.",
                null,
                location.getId());
        emailService.envoyerFinDeBail(location);
        log.info("Location {} terminée, bien {} libéré", locationId, bien.getId());
        return versDTO(location, maintenant);
    }

    /** Dépôt du dernier bail signé du locataire sur le bien (null si aucun, ex. location reprise par V54). */
    private java.math.BigDecimal depotDuBail(Location location) {
        return contratRepository.findFirstByBien_IdAndLocataire_IdAndStatutOrderByCreatedAtDesc(
                        location.getBien().getId(), location.getLocataire().getId(),
                        com.kupanga.api.immobilier.entity.StatutContrat.SIGNE)
                .map(Contrat::getDepotGarantie)
                .orElse(null);
    }

    /** Date du jour dans le fuseau du pays du bien (comme la date de paiement d'une quittance, C8-bis). */
    private java.time.LocalDate aujourdhui(Location location) {
        return java.time.ZonedDateTime.now(clock)
                .withZoneSameInstant(juridictionRegistry.profil(location.getBien().getPays()).fuseau())
                .toLocalDate();
    }

    /** Location active dont le compte est le propriétaire ou le locataire ; sinon 404 (pas d'énumération). */
    private Location locationActiveDe(String email, Long locationId) {
        Location location = locationRepository.findAvecParties(locationId)
                .filter(l -> l.getProprietaire().getMail().equals(email) || l.getLocataire().getMail().equals(email))
                .orElseThrow(LocationServiceImpl::locationIntrouvable);
        if (location.getStatut() != StatutLocation.ACTIVE) {
            throw new KupangaBusinessException("Cette location n'est pas en cours", HttpStatus.CONFLICT);
        }
        return location;
    }

    private static KupangaBusinessException locationIntrouvable() {
        return new KupangaBusinessException("Location introuvable", HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fin d'une réservation : baux non signés du candidat annulés (lien invalidé), puis statut enregistré.
     * Même ordre d'écriture que la signature (bail puis location) : pas d'interblocage entre les deux.
     */
    private void terminer(Location location, StatutLocation statut, LocalDateTime dateFin) {
        contratRepository.annulerNonSignesDuCandidat(location.getBien().getId(), location.getLocataire().getId());
        location.setStatut(statut);
        location.setDateFin(dateFin);
        locationRepository.saveAndFlush(location);
    }

    private static void verifierReservee(Location location, LocalDateTime maintenant) {
        StatutLocation statut = location.statutA(maintenant);
        if (statut == StatutLocation.EXPIREE) {
            throw new KupangaBusinessException("Cette réservation a déjà expiré", HttpStatus.CONFLICT);
        }
        if (statut != StatutLocation.RESERVEE) {
            throw new KupangaBusinessException(
                    statut == StatutLocation.ACTIVE
                            ? "Le bail est déjà signé : la réservation ne peut plus être annulée"
                            : "Cette réservation n'est plus en cours",
                    HttpStatus.CONFLICT);
        }
    }

    private static boolean estLocataireActuel(Contrat contrat) {
        User actuel = contrat.getBien().getLocataire();
        return actuel != null && Objects.equals(actuel.getId(), contrat.getLocataire().getId());
    }

    private LocationDTO versDTO(Location l, LocalDateTime maintenant) {
        Bien bien = l.getBien();
        return new LocationDTO(
                l.getId(),
                bien.getId(),
                bien.getTitre(),
                bien.adresseComplete(),
                bien.getPays(),
                l.getLocataire().getId(),
                nom(l.getLocataire()),
                nom(l.getProprietaire()),
                l.statutA(maintenant),
                l.getDateReservation(),
                l.getDateExpiration(),
                l.getDateDebut(),
                l.getDateFin(),
                l.getDateFinPrevue(),
                l.getCongeDonnePar(),
                l.getDepotRestitue(),
                l.getMotifRetenue());
    }

    private static String nom(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static KupangaBusinessException introuvable() {
        return new KupangaBusinessException("Réservation introuvable", HttpStatus.NOT_FOUND);
    }

    private static KupangaBusinessException plusDisponible() {
        return new KupangaBusinessException("Ce logement n'est plus disponible", HttpStatus.CONFLICT);
    }
}
