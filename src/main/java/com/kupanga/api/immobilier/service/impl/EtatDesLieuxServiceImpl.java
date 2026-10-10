package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.immobilier.dto.formDTO.EtatDesLieuxFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.EtatDesLieuxMapper;
import com.kupanga.api.immobilier.pdf.EtatDesLieuxPdfService;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.EtatDesLieuxService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EtatDesLieuxServiceImpl implements EtatDesLieuxService {

    private final JuridictionRegistry juridictionRegistry;
    private final EtatDesLieuxRepository edlRepository;
    private final EtatDesLieuxPdfService  edlPdfService;
    private final EmailService            emailService;
    private final EtatDesLieuxMapper      edlMapper;
    private final BienService             bienService;
    private final UserService             userService;
    private final NotificationService     notificationService;

    // ─────────────────────────────────────────────────────────────────────────
    // Création
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void creerEtatDesLieux(EtatDesLieuxFormDTO dto, String emailProprietaire) {

        User proprietaire = userService.getUserByEmail(emailProprietaire);
        // Contrôle IDOR : bien du propriétaire connecté + locataire assigné au bien
        Bien bien         = bienService.verifierProprietaire(dto.getBienId(), emailProprietaire);
        bienService.verifierBienActif(bien);
        User locataire    = bienService.verifierLocataireDuBien(bien, dto.getEmailLocataire());

        EtatDesLieux edl = EtatDesLieux.builder()
                .bien(bien)
                // J5 : pays et version du modèle figés (le PDF régénéré à la signature garde ce modèle)
                .pays(bien.getPays())
                .modeleVersion(juridictionRegistry.profil(bien.getPays()).modeleDocuments())
                .proprietaire(proprietaire)
                .locataire(locataire)
                .type(dto.getType())
                .dateRealisation(dto.getDateRealisation())
                .heureRealisation(dto.getHeureRealisation())
                .observations(dto.getObservations())
                .statut(StatutEdl.EN_ATTENTE_SIGNATURE_PROPRIO)
                .build();

        // ← Le builder ignore les initialiseurs inline = new HashSet<>()
        //   sur les entités JPA — initialisation explicite obligatoire
        edl.setPieces(new HashSet<>());
        edl.setCompteurs(new HashSet<>());
        edl.setCles(new HashSet<>());

        if (dto.getPieces() != null) {
            dto.getPieces().forEach(pDto -> edl.getPieces().add(buildPiece(pDto, edl)));
        }

        if (dto.getCompteurs() != null) {
            dto.getCompteurs().forEach(cDto -> edl.getCompteurs().add(buildCompteur(cDto, edl)));
        }

        if (dto.getCles() != null) {
            dto.getCles().forEach(cleDto -> edl.getCles().add(buildCle(cleDto, edl)));
        }

        EtatDesLieux saved = edlRepository.save(edl);
        String clePdf = edlPdfService.genererEtUploaderPdf(saved);
        saved.setClePdf(clePdf);
        edlRepository.save(saved);

        log.info("EDL {} créé pour le bien {}", saved.getId(), bien.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Signature propriétaire
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void signerProprietaire(Long edlId, String signatureBase64, String emailProprietaire) {

        EtatDesLieux edl = findAndVerifyProprietaire(edlId, emailProprietaire);
        bienService.verifierDocumentModifiable(edl.getBien(), edl.getProprietaire(), edl.getLocataire());

        // B6 : un EDL signé est figé ; re-signer en attente du locataire ou après expiration renvoie un lien
        if (edl.getStatut() == StatutEdl.SIGNE) {
            throw new KupangaBusinessException(
                    "Cet état des lieux ne peut plus être signé", HttpStatus.CONFLICT);
        }

        edl.setSignatureProprietaire(signatureBase64);
        edl.setDateSignatureProprietaire(LocalDateTime.now());
        edl.setStatut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        String token = UUID.randomUUID().toString();
        edl.setTokenSignature(token);
        edl.setTokenExpiration(LocalDateTime.now().plusHours(72));

        String clePdf = edlPdfService.genererEtUploaderPdf(edl);
        edl.setClePdf(clePdf);
        // B6 : écriture immédiate : un conflit (409) est détecté avant les e-mails et notifications
        edlRepository.saveAndFlush(edl);

        emailService.envoyerInvitationSignature(edl, token);

        // Notification temps réel au locataire
        notificationService.saveAndSend(
                edl.getLocataire(),
                NotificationType.INVITATION_SIGNATURE_EDL,
                "Vous êtes invité à signer un état des lieux",
                "Le propriétaire " + edl.getProprietaire().getFirstName() + " "
                        + edl.getProprietaire().getLastName()
                        + " vous invite à signer l'état des lieux pour le bien : "
                        + edl.getBien().getAdresse() + ", " + edl.getBien().getVille(),
                token,
                edl.getId()
        );

        log.info("EDL {} signé par le propriétaire, invitation envoyée à {}",
                edlId, edl.getLocataire().getMail());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Signature locataire
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void signerLocataire(String token, String signatureBase64) {

        EtatDesLieux edl = edlRepository.findByTokenSignature(token)
                .orElseThrow(EtatDesLieuxServiceImpl::lienInvalide);

        if (LocalDateTime.now().isAfter(edl.getTokenExpiration())) {
            edlRepository.marquerExpire(edl.getId(), edl.getVersion());
            throw lienExpire();
        }

        if (edl.getStatut() != StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE) {
            throw new KupangaBusinessException(
                    "Cet état des lieux ne peut plus être signé", HttpStatus.CONFLICT);
        }
        bienService.verifierDocumentModifiable(edl.getBien(), edl.getProprietaire(), edl.getLocataire());

        edl.setSignatureLocataire(signatureBase64);
        edl.setDateSignatureLocataire(LocalDateTime.now());
        edl.setStatut(StatutEdl.SIGNE);

        String clePdf = edlPdfService.genererEtUploaderPdf(edl);
        edl.setClePdf(clePdf);

        edl.setTokenSignature(null);
        edl.setTokenExpiration(null);
        // B6 : écriture immédiate : un conflit (409) est détecté avant les e-mails et notifications
        edlRepository.saveAndFlush(edl);

        emailService.envoyerConfirmationEdlSigne(edl);

        // Notifications de confirmation aux deux parties
        notificationService.saveAndSend(
                edl.getLocataire(),
                NotificationType.EDL_SIGNE,
                "État des lieux signé",
                "L'état des lieux pour le bien "
                        + edl.getBien().getAdresse() + ", " + edl.getBien().getVille()
                        + " a été signé par les deux parties.",
                null,
                edl.getId()
        );
        notificationService.saveAndSend(
                edl.getProprietaire(),
                NotificationType.EDL_SIGNE,
                "État des lieux signé",
                "Le locataire " + edl.getLocataire().getFirstName() + " "
                        + edl.getLocataire().getLastName()
                        + " vient de signer l'état des lieux pour le bien "
                        + edl.getBien().getAdresse() + ", " + edl.getBien().getVille() + ".",
                null,
                edl.getId()
        );

        log.info("EDL {} signé par les deux parties — statut : SIGNE", edl.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consultation par token
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public EtatDesLieuxDTO getEdlParToken(String token) {

        EtatDesLieux edl = edlRepository.findByTokenSignature(token)
                .orElseThrow(EtatDesLieuxServiceImpl::lienInvalide);

        if (LocalDateTime.now().isAfter(edl.getTokenExpiration())) {
            edlRepository.marquerExpire(edl.getId(), edl.getVersion());
            throw lienExpire();
        }

        if (edl.getStatut() != StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE) {
            throw new KupangaBusinessException(
                    "Cet état des lieux ne peut plus être signé", HttpStatus.CONFLICT);
        }

        return edlMapper.toDTO(edl);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    // B7 : 404 / 410 et non 401, que le front traite comme une session expirée (retour à la connexion)
    private static KupangaBusinessException lienInvalide() {
        return new KupangaBusinessException("Lien de signature invalide", HttpStatus.NOT_FOUND);
    }

    private static KupangaBusinessException lienExpire() {
        return new KupangaBusinessException("Le lien de signature a expiré", HttpStatus.GONE);
    }

    private EtatDesLieux findAndVerifyProprietaire(Long edlId, String email) {
        EtatDesLieux edl = edlRepository.findWithAllRelations(edlId)
                .orElseThrow(() -> new KupangaBusinessException(
                        "État des lieux introuvable", HttpStatus.NOT_FOUND));
        if (!edl.getProprietaire().getMail().equals(email)) {
            throw new KupangaBusinessException(
                    "Accès non autorisé", HttpStatus.FORBIDDEN);
        }
        return edl;
    }

    private PieceEdl buildPiece(EtatDesLieuxFormDTO.PieceEdlFormDTO dto, EtatDesLieux edl) {
        PieceEdl piece = PieceEdl.builder()
                .nomPiece(dto.getNomPiece())
                .ordre(dto.getOrdre())
                .observations(dto.getObservations())
                .etatDesLieux(edl)
                .build();

        // ← Même problème sur PieceEdl
        piece.setElements(new HashSet<>());

        if (dto.getElements() != null) {
            dto.getElements().forEach(eDto -> {
                ElementEdl element = ElementEdl.builder()
                        .typeElement(eDto.getTypeElement())
                        .etatElement(eDto.getEtatElement())
                        .description(eDto.getDescription())
                        .observation(eDto.getObservation())
                        .piece(piece)
                        .build();
                piece.getElements().add(element);
            });
        }
        return piece;
    }

    private CompteurReleve buildCompteur(EtatDesLieuxFormDTO.CompteurReleveFormDTO dto,
                                         EtatDesLieux edl) {
        return CompteurReleve.builder()
                .typeCompteur(dto.getTypeCompteur())
                .numeroCompteur(dto.getNumeroCompteur())
                .index(dto.getIndex())
                .unite(dto.getUnite())
                .etatDesLieux(edl)
                .build();
    }

    private CleRemise buildCle(EtatDesLieuxFormDTO.CleRemiseFormDTO dto, EtatDesLieux edl) {
        return CleRemise.builder()
                .typeCle(dto.getTypeCle())
                .quantite(dto.getQuantite())
                .etatDesLieux(edl)
                .build();
    }
}