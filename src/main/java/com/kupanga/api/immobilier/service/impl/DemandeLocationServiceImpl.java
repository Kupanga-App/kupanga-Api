package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.chat.repository.ConversationRepository;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.DemandeLocationDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.DemandeLocation;
import com.kupanga.api.immobilier.entity.StatutDemandeLocation;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.DemandeLocationRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.DemandeLocationService;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class DemandeLocationServiceImpl implements DemandeLocationService {

    private final DemandeLocationRepository demandeRepository;
    private final BienRepository bienRepository;
    private final ConversationRepository conversationRepository;
    private final BienService bienService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final Clock clock;

    @Autowired
    public DemandeLocationServiceImpl(DemandeLocationRepository demandeRepository, BienRepository bienRepository,
                                      ConversationRepository conversationRepository, BienService bienService,
                                      UserService userService, NotificationService notificationService,
                                      EmailService emailService) {
        this(demandeRepository, bienRepository, conversationRepository, bienService, userService,
                notificationService, emailService, Clock.systemDefaultZone());
    }

    /** Horloge fixée par les tests (expiration des demandes). */
    DemandeLocationServiceImpl(DemandeLocationRepository demandeRepository, BienRepository bienRepository,
                               ConversationRepository conversationRepository, BienService bienService,
                               UserService userService, NotificationService notificationService,
                               EmailService emailService, Clock clock) {
        this.demandeRepository = demandeRepository;
        this.bienRepository = bienRepository;
        this.conversationRepository = conversationRepository;
        this.bienService = bienService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DemandeLocationDTO envoyer(String emailProprietaire, Long bienId, Long locataireId) {
        User proprietaire = userService.getUserByEmail(emailProprietaire);
        userService.verifyIfUserIsOwner(proprietaire.getRole());
        Bien bien = bienService.verifierProprietaire(bienId, proprietaire.getMail());
        bienService.verifierBienActif(bien);
        if (bien.getLocataire() != null) {
            throw new KupangaBusinessException(
                    "Ce bien a déjà un locataire : libérez-le avant de le proposer à quelqu'un d'autre",
                    HttpStatus.CONFLICT);
        }
        User locataire = candidat(bien, proprietaire, locataireId);

        LocalDateTime maintenant = LocalDateTime.now(clock);
        demandeRepository.expirerEchuesDuBien(bienId, maintenant);
        if (demandeRepository.existsByBien_IdAndStatut(bienId, StatutDemandeLocation.EN_ATTENTE)) {
            throw demandeDejaEnAttente();
        }

        DemandeLocation demande = DemandeLocation.builder()
                .bien(bien).proprietaire(proprietaire).locataire(locataire)
                .statut(StatutDemandeLocation.EN_ATTENTE)
                .dateCreation(maintenant)
                .dateExpiration(maintenant.plus(VALIDITE))
                .build();
        try {
            demande = demandeRepository.saveAndFlush(demande);
        } catch (DataIntegrityViolationException e) {
            // Deux envois simultanés sur le même bien : l'index unique partiel (V53) en refuse un
            throw demandeDejaEnAttente();
        }

        notificationService.saveAndSend(
                locataire,
                NotificationType.DEMANDE_LOCATION,
                "Nouvelle demande de location",
                nom(proprietaire) + " vous propose de devenir locataire du logement situé au "
                        + bien.adresseComplete() + ". Répondez avant 72 heures.",
                null,
                demande.getId());
        emailService.envoyerDemandeLocation(demande);
        log.info("Demande de location {} envoyée pour le bien {}", demande.getId(), bienId);
        return versDTO(demande, maintenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeLocationDTO> duBien(String emailProprietaire, Long bienId) {
        User proprietaire = userService.getUserByEmail(emailProprietaire);
        userService.verifyIfUserIsOwner(proprietaire.getRole());
        bienService.verifierProprietaire(bienId, proprietaire.getMail());
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return demandeRepository.findDuBien(bienId).stream().map(d -> versDTO(d, maintenant)).toList();
    }

    @Override
    @Transactional
    public void annuler(String emailProprietaire, Long demandeId) {
        DemandeLocation demande = demandeRepository.findAvecParties(demandeId)
                .orElseThrow(DemandeLocationServiceImpl::introuvable);
        if (!demande.getProprietaire().getMail().equals(emailProprietaire)) {
            throw new KupangaBusinessException("Accès non autorisé", HttpStatus.FORBIDDEN);
        }
        LocalDateTime maintenant = LocalDateTime.now(clock);
        if (demande.statutA(maintenant) != StatutDemandeLocation.EN_ATTENTE) {
            throw new KupangaBusinessException("Cette demande n'est plus en attente", HttpStatus.CONFLICT);
        }
        demande.setStatut(StatutDemandeLocation.ANNULEE);
        demande.setDateReponse(maintenant);
        demandeRepository.saveAndFlush(demande);

        notificationService.saveAndSend(
                demande.getLocataire(),
                NotificationType.DEMANDE_LOCATION_ANNULEE,
                "Demande de location retirée",
                nom(demande.getProprietaire()) + " a retiré sa demande pour le logement situé au "
                        + demande.getBien().adresseComplete() + ".",
                null,
                demande.getId());
        log.info("Demande de location {} annulée par le propriétaire", demandeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandeLocationDTO> recues(String emailLocataire) {
        User locataire = userService.getUserByEmail(emailLocataire);
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return demandeRepository.findRecuesEnAttente(locataire.getId(), maintenant).stream()
                .map(d -> versDTO(d, maintenant)).toList();
    }

    @Override
    @Transactional
    public DemandeLocationDTO accepter(String emailLocataire, Long demandeId) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        DemandeLocation demande = demandeDuLocataire(emailLocataire, demandeId, maintenant);
        Bien bien = demande.getBien();
        bienService.verifierBienActif(bien);
        if (bien.getLocataire() != null) {
            throw new KupangaBusinessException("Ce logement n'est plus disponible", HttpStatus.CONFLICT);
        }

        demande.setStatut(StatutDemandeLocation.ACCEPTEE);
        demande.setDateReponse(maintenant);
        demandeRepository.saveAndFlush(demande);
        bien.setLocataire(demande.getLocataire());
        bienRepository.save(bien);

        notificationService.saveAndSend(
                demande.getProprietaire(),
                NotificationType.DEMANDE_LOCATION_ACCEPTEE,
                "Demande de location acceptée",
                nom(demande.getLocataire()) + " a accepté votre demande. Le logement situé au "
                        + bien.adresseComplete() + " lui est maintenant attribué : vous pouvez préparer le bail.",
                null,
                bien.getId());
        log.info("Demande de location {} acceptée, bien {} assigné", demandeId, bien.getId());
        return versDTO(demande, maintenant);
    }

    @Override
    @Transactional
    public DemandeLocationDTO refuser(String emailLocataire, Long demandeId) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        DemandeLocation demande = demandeDuLocataire(emailLocataire, demandeId, maintenant);

        demande.setStatut(StatutDemandeLocation.REFUSEE);
        demande.setDateReponse(maintenant);
        demandeRepository.saveAndFlush(demande);

        notificationService.saveAndSend(
                demande.getProprietaire(),
                NotificationType.DEMANDE_LOCATION_REFUSEE,
                "Demande de location refusée",
                nom(demande.getLocataire()) + " a refusé votre demande pour le logement situé au "
                        + demande.getBien().adresseComplete() + ".",
                null,
                demande.getBien().getId());
        log.info("Demande de location {} refusée", demandeId);
        return versDTO(demande, maintenant);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Seuls les candidats du bien (conversation avec le propriétaire sur ce bien) peuvent recevoir une demande :
     * sinon un propriétaire pourrait viser n'importe quel compte. Id inconnu, compte anonymisé et non-candidat
     * donnent la même réponse : rien n'indique si le compte existe.
     */
    private User candidat(Bien bien, User proprietaire, Long locataireId) {
        User locataire;
        try {
            locataire = userService.findById(locataireId);
        } catch (KupangaBusinessException e) {
            throw candidatIntrouvable();
        }
        if (locataire.isAnonymise() || conversationRepository.findConversationWithBienIdAndEmailExpediteur(
                bien.getId(), proprietaire.getMail(), locataire.getMail()).isEmpty()) {
            throw candidatIntrouvable();
        }
        if (locataire.getRole() != Role.ROLE_LOCATAIRE) {
            throw new KupangaBusinessException(
                    "La demande ne peut être envoyée qu'à un compte locataire", HttpStatus.BAD_REQUEST);
        }
        return locataire;
    }

    /** Demande adressée au locataire connecté et encore en attente ; celle d'un autre compte donne 404. */
    private DemandeLocation demandeDuLocataire(String emailLocataire, Long demandeId, LocalDateTime maintenant) {
        DemandeLocation demande = demandeRepository.findAvecParties(demandeId)
                .filter(d -> d.getLocataire().getMail().equals(emailLocataire))
                .orElseThrow(DemandeLocationServiceImpl::introuvable);
        StatutDemandeLocation statut = demande.statutA(maintenant);
        if (statut == StatutDemandeLocation.EXPIREE) {
            throw new KupangaBusinessException(
                    "Cette demande a expiré : demandez au propriétaire de vous en envoyer une nouvelle", HttpStatus.GONE);
        }
        if (statut != StatutDemandeLocation.EN_ATTENTE) {
            throw new KupangaBusinessException("Cette demande n'est plus en attente", HttpStatus.CONFLICT);
        }
        return demande;
    }

    private DemandeLocationDTO versDTO(DemandeLocation d, LocalDateTime maintenant) {
        Bien bien = d.getBien();
        return new DemandeLocationDTO(
                d.getId(),
                bien.getId(),
                bien.getTitre(),
                bien.adresseComplete(),
                bien.getPays(),
                d.getLocataire().getId(),
                nom(d.getLocataire()),
                nom(d.getProprietaire()),
                d.statutA(maintenant),
                d.getDateCreation(),
                d.getDateExpiration(),
                d.getDateReponse());
    }

    private static String nom(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static KupangaBusinessException introuvable() {
        return new KupangaBusinessException("Demande de location introuvable", HttpStatus.NOT_FOUND);
    }

    private static KupangaBusinessException candidatIntrouvable() {
        return new KupangaBusinessException("Locataire introuvable pour ce bien", HttpStatus.NOT_FOUND);
    }

    private static KupangaBusinessException demandeDejaEnAttente() {
        return new KupangaBusinessException(
                "Une demande est déjà en attente pour ce bien : retirez-la avant d'en envoyer une autre",
                HttpStatus.CONFLICT);
    }
}
