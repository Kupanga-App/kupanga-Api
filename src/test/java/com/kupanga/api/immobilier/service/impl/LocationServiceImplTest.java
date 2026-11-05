package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.LocationDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.Location;
import com.kupanga.api.immobilier.entity.StatutLocation;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.LocationRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.LocationService;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService : réservation jusqu'à la signature du bail (décision du 2026-11-05)")
class LocationServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final Instant INSTANT = Instant.parse("2026-11-05T10:00:00Z");
    private static final LocalDateTime MAINTENANT = LocalDateTime.ofInstant(INSTANT, ZONE);

    @Mock private LocationRepository locationRepository;
    @Mock private BienRepository bienRepository;
    @Mock private ContratRepository contratRepository;
    @Mock private BienService bienService;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    private LocationService service;
    private User proprietaire;
    private User candidat;
    private Bien bien;

    /** Exécute la tâche sans transaction réelle. */
    private static final class TransactionDirecte extends TransactionTemplate {
        @Override
        public <T> T execute(TransactionCallback<T> action) throws TransactionException {
            return action.doInTransaction(null);
        }
    }

    @BeforeEach
    void setUp() {
        service = new LocationServiceImpl(locationRepository, bienRepository, contratRepository, bienService,
                notificationService, emailService, com.kupanga.api.juridiction.JuridictionsDeTest.registre(),
                new TransactionDirecte(), Clock.fixed(INSTANT, ZONE));
        proprietaire = User.builder().id(1L).firstName("Paul").lastName("Proprio").mail("proprio@test.fr").build();
        candidat = User.builder().id(2L).firstName("Lea").lastName("Loc").mail("loc@test.fr").build();
        bien = Bien.builder().id(10L).titre("T2").adresse("12 rue A").codePostal("44000").ville("Nantes")
                .pays(Pays.FR).proprietaire(proprietaire).build();
    }

    private Location reservation(LocalDateTime expiration) {
        return Location.builder().id(80L).bien(bien).proprietaire(proprietaire).locataire(candidat)
                .statut(StatutLocation.RESERVEE).dateReservation(expiration.minusDays(14))
                .dateExpiration(expiration).build();
    }

    private Contrat bailDe(User locataire) {
        return Contrat.builder().id(30L).bien(bien).proprietaire(proprietaire).locataire(locataire).build();
    }

    // ── Bien libre, réservation ──────────────────────────────────────────────

    @Test
    @DisplayName("verifierBienLibre : réservations échues expirées d'abord ; locataire ou réservation en cours → 409")
    void verifierBienLibre() {
        assertThatCode(() -> service.verifierBienLibre(bien, MAINTENANT)).doesNotThrowAnyException();
        verify(locationRepository).findIdsReservationsEchuesDuBien(10L, MAINTENANT);

        when(locationRepository.findReservationValable(10L, MAINTENANT))
                .thenReturn(Optional.of(reservation(MAINTENANT.plusDays(3))));
        assertThatThrownBy(() -> service.verifierBienLibre(bien, MAINTENANT))
                .hasMessageContaining("réservé").extracting("status").isEqualTo(HttpStatus.CONFLICT);

        bien.setLocataire(User.builder().id(7L).build());
        assertThatThrownBy(() -> service.verifierBienLibre(bien, MAINTENANT))
                .hasMessageContaining("déjà un locataire").extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("reserver : RESERVEE pour 14 jours, sans assigner le bien ; index unique (concurrence) → 409")
    void reserver() {
        DemandeLocation demande = DemandeLocation.builder().id(50L).bien(bien).proprietaire(proprietaire)
                .locataire(candidat).build();
        when(locationRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        Location l = service.reserver(demande, MAINTENANT);

        assertThat(l.getStatut()).isEqualTo(StatutLocation.RESERVEE);
        assertThat(l.getDateExpiration()).isEqualTo(MAINTENANT.plusDays(14));
        assertThat(l.getLocataire()).isEqualTo(candidat);
        assertThat(l.getDemande()).isEqualTo(demande);
        assertThat(bien.getLocataire()).isNull();

        when(locationRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("ux"));
        assertThatThrownBy(() -> service.reserver(demande, MAINTENANT))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    // ── Bail ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("locataireDuBail : locataire actuel ou candidat qui a réservé (casse ignorée) ; autre → 400")
    void locataireDuBail() {
        when(locationRepository.findReservationValable(10L, MAINTENANT))
                .thenReturn(Optional.of(reservation(MAINTENANT.plusDays(3))));
        assertThat(service.locataireDuBail(bien, "LOC@test.fr")).isEqualTo(candidat);

        assertThatThrownBy(() -> service.locataireDuBail(bien, "autre@test.fr"))
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.locataireDuBail(bien, null))
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        User actuel = User.builder().id(7L).mail("actuel@test.fr").build();
        bien.setLocataire(actuel);
        assertThat(service.locataireDuBail(bien, "actuel@test.fr")).isEqualTo(actuel);
    }

    @Test
    @DisplayName("verifierBailSignable : bail du candidat sans réservation valable → 409 ; locataire actuel → toujours")
    void verifierBailSignable() {
        when(locationRepository.findReservationValable(10L, MAINTENANT))
                .thenReturn(Optional.of(reservation(MAINTENANT.plusDays(3))));
        assertThatCode(() -> service.verifierBailSignable(bailDe(candidat))).doesNotThrowAnyException();

        User autre = User.builder().id(9L).mail("autre@test.fr").build();
        assertThatThrownBy(() -> service.verifierBailSignable(bailDe(autre)))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        when(locationRepository.findReservationValable(10L, MAINTENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifierBailSignable(bailDe(candidat)))
                .hasMessageContaining("réservation").extracting("status").isEqualTo(HttpStatus.CONFLICT);

        bien.setLocataire(candidat);
        assertThatCode(() -> service.verifierBailSignable(bailDe(candidat))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("activerApresSignature : la réservation devient ACTIVE, le candidat devient locataire et est prévenu")
    void activerApresSignature() {
        Location r = reservation(MAINTENANT.plusDays(3));
        when(locationRepository.findReservationValable(10L, MAINTENANT)).thenReturn(Optional.of(r));

        service.activerApresSignature(bailDe(candidat));

        assertThat(r.getStatut()).isEqualTo(StatutLocation.ACTIVE);
        assertThat(r.getDateDebut()).isEqualTo(MAINTENANT);
        assertThat(bien.getLocataire()).isEqualTo(candidat);
        verify(locationRepository).saveAndFlush(r);
        verify(bienRepository).save(bien);
        verify(notificationService).saveAndSend(eq(candidat), eq(NotificationType.BIEN_ASSIGNE),
                anyString(), anyString(), isNull(), eq(10L));
    }

    @Test
    @DisplayName("activerApresSignature : bail du locataire actuel (renouvellement) → rien ne change")
    void activerApresSignature_locataireActuel() {
        bien.setLocataire(candidat);

        service.activerApresSignature(bailDe(candidat));

        verifyNoInteractions(locationRepository, bienRepository, notificationService);
    }

    // ── Désistement et retrait ───────────────────────────────────────────────

    @Test
    @DisplayName("desister : DESISTEE, baux non signés annulés, propriétaire prévenu")
    void desister() {
        Location r = reservation(MAINTENANT.plusDays(3));
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(r));

        LocationDTO dto = service.desister("loc@test.fr", 80L);

        assertThat(dto.statut()).isEqualTo(StatutLocation.DESISTEE);
        assertThat(r.getDateFin()).isEqualTo(MAINTENANT);
        verify(locationRepository).saveAndFlush(r);
        verify(contratRepository).annulerNonSignesDuCandidat(10L, 2L);
        verify(notificationService).saveAndSend(eq(proprietaire), eq(NotificationType.RESERVATION_DESISTEE),
                anyString(), anyString(), isNull(), eq(10L));
        verify(emailService).envoyerFinReservation(r, StatutLocation.DESISTEE, proprietaire);
        assertThat(bien.getLocataire()).isNull();
    }

    @Test
    @DisplayName("desister : réservation d'un autre → 404 ; expirée ou bail signé → 409 ; rien n'est modifié")
    void desister_refus() {
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(reservation(MAINTENANT.plusDays(3))));
        assertThatThrownBy(() -> service.desister("proprio@test.fr", 80L))
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(reservation(MAINTENANT)));
        assertThatThrownBy(() -> service.desister("loc@test.fr", 80L))
                .hasMessageContaining("expiré").extracting("status").isEqualTo(HttpStatus.CONFLICT);

        Location active = reservation(MAINTENANT.plusDays(3));
        active.setStatut(StatutLocation.ACTIVE);
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> service.desister("loc@test.fr", 80L))
                .hasMessageContaining("bail est déjà signé").extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(locationRepository, never()).saveAndFlush(any());
        verifyNoInteractions(contratRepository, notificationService, emailService);
    }

    @Test
    @DisplayName("annuler : le propriétaire retire la réservation (ANNULEE, baux annulés, candidat prévenu) ; autre → 404")
    void annuler() {
        Location r = reservation(MAINTENANT.plusDays(3));
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.annuler("loc@test.fr", 80L))
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(service.annuler("proprio@test.fr", 80L).statut()).isEqualTo(StatutLocation.ANNULEE);
        verify(contratRepository).annulerNonSignesDuCandidat(10L, 2L);
        verify(notificationService).saveAndSend(eq(candidat), eq(NotificationType.RESERVATION_ANNULEE),
                anyString(), anyString(), isNull(), eq(80L));
        verify(emailService).envoyerFinReservation(r, StatutLocation.ANNULEE, candidat);
    }

    // ── Expiration ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("expirerReservationsEchues : EXPIREE, baux annulés, deux parties prévenues ; déjà signée → ignorée")
    void expirerReservationsEchues() {
        Location echue = reservation(MAINTENANT.minusHours(1));
        Location signeeEntreTemps = reservation(MAINTENANT.minusHours(1));
        signeeEntreTemps.setId(81L);
        signeeEntreTemps.setStatut(StatutLocation.ACTIVE);
        when(locationRepository.findIdsReservationsEchues(MAINTENANT)).thenReturn(List.of(80L, 81L, 82L));
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(echue));
        when(locationRepository.findAvecParties(81L)).thenReturn(Optional.of(signeeEntreTemps));
        when(locationRepository.findAvecParties(82L)).thenThrow(new IllegalStateException("verrou"));

        assertThat(service.expirerReservationsEchues()).isEqualTo(1);

        assertThat(echue.getStatut()).isEqualTo(StatutLocation.EXPIREE);
        assertThat(echue.getDateFin()).isEqualTo(echue.getDateExpiration());
        assertThat(signeeEntreTemps.getStatut()).isEqualTo(StatutLocation.ACTIVE);
        verify(contratRepository).annulerNonSignesDuCandidat(10L, 2L);
        verify(notificationService).saveAndSend(eq(proprietaire), eq(NotificationType.RESERVATION_EXPIREE),
                anyString(), anyString(), isNull(), eq(10L));
        verify(notificationService).saveAndSend(eq(candidat), eq(NotificationType.RESERVATION_EXPIREE_CANDIDAT),
                anyString(), anyString(), isNull(), eq(80L));
        verify(emailService).envoyerFinReservation(echue, StatutLocation.EXPIREE, proprietaire);
        verify(emailService).envoyerFinReservation(echue, StatutLocation.EXPIREE, candidat);
    }

    @Test
    @DisplayName("verifierBienLibre : une réservation échue est expirée en entier (bail annulé avant, parties prévenues)")
    void verifierBienLibre_expireCompletement() {
        Location echue = reservation(MAINTENANT.minusHours(1));
        when(locationRepository.findIdsReservationsEchuesDuBien(10L, MAINTENANT)).thenReturn(List.of(80L));
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(echue));

        service.verifierBienLibre(bien, MAINTENANT);

        assertThat(echue.getStatut()).isEqualTo(StatutLocation.EXPIREE);
        var ordre = inOrder(contratRepository, locationRepository);
        ordre.verify(contratRepository).annulerNonSignesDuCandidat(10L, 2L);
        ordre.verify(locationRepository).saveAndFlush(echue);
        verify(notificationService).saveAndSend(eq(proprietaire), eq(NotificationType.RESERVATION_EXPIREE),
                anyString(), anyString(), isNull(), eq(10L));
        verify(notificationService).saveAndSend(eq(candidat), eq(NotificationType.RESERVATION_EXPIREE_CANDIDAT),
                anyString(), anyString(), isNull(), eq(80L));
    }

    // ── Fin de bail ──────────────────────────────────────────────────────────

    private Location active() {
        Location l = reservation(MAINTENANT.plusDays(3));
        l.setStatut(StatutLocation.ACTIVE);
        l.setDateDebut(MAINTENANT.minusMonths(6));
        return l;
    }

    @Test
    @DisplayName("donnerConge : par le locataire → propriétaire prévenu (notification + e-mail) ; date bornée ; un seul congé")
    void donnerConge() {
        Location l = active();
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(l));
        java.time.LocalDate dans1Mois = MAINTENANT.toLocalDate().plusMonths(1);

        LocationDTO dto = service.donnerConge("loc@test.fr", 80L, dans1Mois);

        assertThat(dto.dateFinPrevue()).isEqualTo(dans1Mois);
        assertThat(dto.congeDonnePar()).isEqualTo(com.kupanga.api.immobilier.entity.PartieLocation.LOCATAIRE);
        verify(notificationService).saveAndSend(eq(proprietaire), eq(NotificationType.CONGE_POUR_PROPRIETAIRE),
                anyString(), anyString(), isNull(), eq(10L));
        verify(emailService).envoyerConge(l, proprietaire, false);

        assertThatThrownBy(() -> service.donnerConge("proprio@test.fr", 80L, dans1Mois))
                .hasMessageContaining("déjà donné").extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("donnerConge : date passée ou au-delà de 2 ans → 400 ; compte étranger → 404 ; location pas active → 409")
    void donnerConge_refus() {
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(active()));
        java.time.LocalDate aujourdhui = MAINTENANT.toLocalDate();
        assertThatThrownBy(() -> service.donnerConge("proprio@test.fr", 80L, aujourdhui.minusDays(1)))
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.donnerConge("proprio@test.fr", 80L, aujourdhui.plusYears(2).plusDays(1)))
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.donnerConge("intrus@test.fr", 80L, aujourdhui))
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        when(locationRepository.findAvecParties(81L)).thenReturn(Optional.of(reservation(MAINTENANT.plusDays(3))));
        assertThatThrownBy(() -> service.donnerConge("proprio@test.fr", 81L, aujourdhui))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(notificationService, emailService);
    }

    @Test
    @DisplayName("donnerConge : bien archivé ou partie anonymisée (B12) → 409, rien n'est envoyé")
    void donnerConge_bienArchive_409() {
        Location l = active();
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(l));
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierDocumentModifiable(bien, proprietaire, candidat);

        assertThatThrownBy(() -> service.donnerConge("loc@test.fr", 80L, MAINTENANT.toLocalDate().plusMonths(1)))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        assertThat(l.getDateFinPrevue()).isNull();
        verifyNoInteractions(notificationService, emailService);
    }

    @Test
    @DisplayName("retirerConge : seulement par son auteur ; l'autre partie est prévenue")
    void retirerConge() {
        Location l = active();
        l.setDateFinPrevue(MAINTENANT.toLocalDate().plusMonths(1));
        l.setCongeDonnePar(com.kupanga.api.immobilier.entity.PartieLocation.PROPRIETAIRE);
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.retirerConge("loc@test.fr", 80L))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        service.retirerConge("proprio@test.fr", 80L);
        assertThat(l.getDateFinPrevue()).isNull();
        assertThat(l.getCongeDonnePar()).isNull();
        verify(notificationService).saveAndSend(eq(candidat), eq(NotificationType.CONGE_POUR_LOCATAIRE),
                anyString(), anyString(), isNull(), eq(10L));
        verify(emailService).envoyerConge(l, candidat, true);
    }

    @Test
    @DisplayName("terminer : sans congé / avant la date → 409 ; au-delà du dépôt → 400 ; retenue sans motif → 400")
    void terminer_refus() {
        Location l = active();
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(l));
        java.math.BigDecimal depot = new java.math.BigDecimal("700.00");
        when(contratRepository.findFirstByBien_IdAndLocataire_IdAndStatutOrderByCreatedAtDesc(
                10L, 2L, com.kupanga.api.immobilier.entity.StatutContrat.SIGNE))
                .thenReturn(Optional.of(Contrat.builder().depotGarantie(depot).build()));

        assertThatThrownBy(() -> service.terminer("proprio@test.fr", 80L, depot, null))
                .hasMessageContaining("congé").extracting("status").isEqualTo(HttpStatus.CONFLICT);
        l.setCongeDonnePar(com.kupanga.api.immobilier.entity.PartieLocation.LOCATAIRE);
        l.setDateFinPrevue(MAINTENANT.toLocalDate().plusDays(1));
        assertThatThrownBy(() -> service.terminer("proprio@test.fr", 80L, depot, null))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        l.setDateFinPrevue(MAINTENANT.toLocalDate());
        assertThatThrownBy(() -> service.terminer("loc@test.fr", 80L, depot, null))
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> service.terminer("proprio@test.fr", 80L, new java.math.BigDecimal("700.01"), null))
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> service.terminer("proprio@test.fr", 80L, new java.math.BigDecimal("500"), "  "))
                .hasMessageContaining("motif").extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(l.getStatut()).isEqualTo(StatutLocation.ACTIVE);
        verifyNoInteractions(notificationService, emailService);
    }

    @Test
    @DisplayName("terminer : TERMINEE, dépôt et motif enregistrés, bien libéré, baux non signés annulés, locataire prévenu")
    void terminer() {
        Location l = active();
        l.setCongeDonnePar(com.kupanga.api.immobilier.entity.PartieLocation.LOCATAIRE);
        l.setDateFinPrevue(MAINTENANT.toLocalDate());
        bien.setLocataire(candidat);
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(l));
        when(locationRepository.saveAndFlush(l)).thenReturn(l);
        when(bienRepository.findById(10L)).thenReturn(Optional.of(bien));
        when(contratRepository.findFirstByBien_IdAndLocataire_IdAndStatutOrderByCreatedAtDesc(
                10L, 2L, com.kupanga.api.immobilier.entity.StatutContrat.SIGNE))
                .thenReturn(Optional.of(Contrat.builder().depotGarantie(new java.math.BigDecimal("700.00")).build()));

        LocationDTO dto = service.terminer("proprio@test.fr", 80L, new java.math.BigDecimal("500"), " Peinture ");

        assertThat(dto.statut()).isEqualTo(StatutLocation.TERMINEE);
        assertThat(l.getDepotRestitue()).isEqualByComparingTo("500");
        assertThat(l.getMotifRetenue()).isEqualTo("Peinture");
        assertThat(bien.getLocataire()).isNull();
        var ordre = inOrder(contratRepository, locationRepository);
        ordre.verify(contratRepository).annulerNonSignesDuCandidat(10L, 2L);
        ordre.verify(locationRepository).saveAndFlush(l);
        verify(bienRepository).save(bien);
        verify(notificationService).saveAndSend(eq(candidat), eq(NotificationType.LOCATION_TERMINEE),
                anyString(), anyString(), isNull(), eq(80L));
        verify(emailService).envoyerFinDeBail(l);
    }

    @Test
    @DisplayName("terminer : dépôt rendu en entier → motif ignoré ; location reprise sans bail signé → pas de plafond")
    void terminer_depotEntier() {
        Location l = active();
        l.setCongeDonnePar(com.kupanga.api.immobilier.entity.PartieLocation.PROPRIETAIRE);
        l.setDateFinPrevue(MAINTENANT.toLocalDate().minusDays(3));
        when(locationRepository.findAvecParties(80L)).thenReturn(Optional.of(l));
        when(locationRepository.saveAndFlush(l)).thenReturn(l);
        when(bienRepository.findById(10L)).thenReturn(Optional.of(bien));
        when(contratRepository.findFirstByBien_IdAndLocataire_IdAndStatutOrderByCreatedAtDesc(
                10L, 2L, com.kupanga.api.immobilier.entity.StatutContrat.SIGNE))
                .thenReturn(Optional.of(Contrat.builder().depotGarantie(new java.math.BigDecimal("700.00")).build()));

        service.terminer("proprio@test.fr", 80L, new java.math.BigDecimal("700.00"), "inutile");

        assertThat(l.getMotifRetenue()).isNull();
    }

    @Test
    @DisplayName("statut affiché : une réservation dont l'échéance est passée est EXPIREE")
    void statutAffiche() {
        assertThat(reservation(MAINTENANT).statutA(MAINTENANT)).isEqualTo(StatutLocation.EXPIREE);
        assertThat(reservation(MAINTENANT.plusSeconds(1)).statutA(MAINTENANT)).isEqualTo(StatutLocation.RESERVEE);
    }

    @Test
    @DisplayName("enCoursDuBien : propriétaire vérifié, réservation sinon location active, sinon vide")
    void enCoursDuBien() {
        when(locationRepository.findReservationValable(10L, MAINTENANT))
                .thenReturn(Optional.of(reservation(MAINTENANT.plusDays(3))));
        assertThat(service.enCoursDuBien("proprio@test.fr", 10L)).get()
                .extracting(LocationDTO::statut).isEqualTo(StatutLocation.RESERVEE);
        verify(bienService).verifierProprietaire(10L, "proprio@test.fr");

        when(locationRepository.findReservationValable(10L, MAINTENANT)).thenReturn(Optional.empty());
        when(locationRepository.findActive(10L)).thenReturn(Optional.empty());
        assertThat(service.enCoursDuBien("proprio@test.fr", 10L)).isEmpty();

        doThrow(new KupangaBusinessException("Accès refusé", HttpStatus.FORBIDDEN))
                .when(bienService).verifierProprietaire(10L, "intrus@test.fr");
        assertThatThrownBy(() -> service.enCoursDuBien("intrus@test.fr", 10L))
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }
}
