package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.ContratFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.mapper.ContratMapper;
import com.kupanga.api.immobilier.pdf.ContratPdfService;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.ContratService;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContratServiceImpl implements ContratService {

    private final ContratRepository  contratRepository;
    private final ContratPdfService  contratPdfService;
    private final EmailService       emailService;
    private final UserService        userService;
    private final BienService        bienService;
    private final ContratMapper      contratMapper;
    private final NotificationService notificationService;
    private final JuridictionRegistry juridictionRegistry;

    @Override
    public void creerContrat(ContratFormDTO dto, String emailProprietaire) {

        User proprietaire = userService.getUserByEmail(emailProprietaire);

        // Contrôle IDOR : le bien doit appartenir au propriétaire connecté,
        // et le locataire doit être celui assigné au bien
        Bien bien = bienService.verifierProprietaire(dto.getBienId(), emailProprietaire);
        bienService.verifierBienActif(bien);
        User locataire = bienService.verifierLocataireDuBien(bien, dto.getEmailLocataire());

        // C5 : montants du bail sous les plafonds de la devise du bien
        juridictionRegistry.verifierMontants(bien.getDevise(), dto.getLoyerMensuel(), dto.getChargesMensuelles(),
                dto.getDepotGarantie());

        Contrat contrat = Contrat.builder()
                .bien(bien)
                // J3 : juridiction figée sur le bail (ne change plus si le profil évolue)
                .pays(bien.getPays())
                .devise(bien.getDevise())
                .modeleVersion(juridictionRegistry.profil(bien.getPays()).modeleDocuments())
                .proprietaire(proprietaire)
                .locataire(locataire)
                .adresseBien(bien.getAdresse() + ", " + bien.getVille())
                .loyerMensuel(dto.getLoyerMensuel())
                .chargesMensuelles(dto.getChargesMensuelles())
                .depotGarantie(dto.getDepotGarantie())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .dureeBailMois(dto.getDureeBailMois())
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_PROPRIO)
                .build();

        // Génère le PDF initial (sans signatures)
        String clePdf = contratPdfService.genererEtUploaderPdf(contrat);
        contrat.setClePdf(clePdf);
        contratRepository.save(contrat);

    }

    public ContratDTO getContratParToken(String token) {

        Contrat contrat = contratRepository.findByTokenSignature(token)
                .orElseThrow(ContratServiceImpl::lienInvalide);

        // Vérifie si le token est expiré
        if (LocalDateTime.now().isAfter(contrat.getTokenExpiration())) {
            contratRepository.marquerExpire(contrat.getId(), contrat.getVersion());
            throw lienExpire();
        }

        // Vérifie que le contrat est bien en attente de signature locataire
        if (contrat.getStatut() != StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE) {
            throw new KupangaBusinessException("Ce contrat ne peut plus être signé", HttpStatus.CONFLICT);
        }

        return contratMapper.toDTO(contrat);
    }

    @Override
    public void signerProprietaire(Long contratId, String signatureBase64,
                                   String emailProprietaire) {

        Contrat contrat = findAndVerify(contratId, emailProprietaire);
        bienService.verifierDocumentModifiable(contrat.getBien(), contrat.getProprietaire(), contrat.getLocataire());

        // B6 : un contrat signé (ou annulé) est figé. Re-signer en attente du locataire ou après expiration
        // reste possible : c'est la seule façon de renvoyer un lien de signature (l'ancien est invalidé).
        if (contrat.getStatut() == StatutContrat.SIGNE || contrat.getStatut() == StatutContrat.ANNULE) {
            throw new KupangaBusinessException("Ce contrat ne peut plus être signé", HttpStatus.CONFLICT);
        }

        contrat.setSignatureProprietaire(signatureBase64);
        contrat.setDateSignatureProprietaire(LocalDateTime.now());
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        // Génère un token unique pour le locataire
        String token = UUID.randomUUID().toString();
        contrat.setTokenSignature(token);
        contrat.setTokenExpiration(LocalDateTime.now().plusHours(72));

        // Regénère le PDF avec la signature du proprio
        String clePdf = contratPdfService.genererEtUploaderPdf(contrat);
        contrat.setClePdf(clePdf);
        // B6 : écriture immédiate : un conflit (409) est détecté avant les e-mails et notifications
        contratRepository.saveAndFlush(contrat);

        // Envoie l'email au locataire
        emailService.envoyerInvitationSignature(contrat, token);

        // Notification temps réel au locataire
        notificationService.saveAndSend(
                contrat.getLocataire(),
                NotificationType.INVITATION_SIGNATURE_CONTRAT,
                "Vous êtes invité à signer un contrat",
                "Le propriétaire " + contrat.getProprietaire().getFirstName() + " "
                        + contrat.getProprietaire().getLastName()
                        + " vous invite à signer le contrat pour le bien : "
                        + contrat.getAdresseBien(),
                token,
                contrat.getId()
        );
    }

    @Override
    public void signerLocataire(String token, String signatureBase64) {

        Contrat contrat = contratRepository.findByTokenSignature(token)
                .orElseThrow(ContratServiceImpl::lienInvalide);

        // Vérifie l'expiration
        if (LocalDateTime.now().isAfter(contrat.getTokenExpiration())) {
            contratRepository.marquerExpire(contrat.getId(), contrat.getVersion());
            throw lienExpire();
        }

        // B6 : le locataire ne signe qu'après le propriétaire, et une seule fois
        if (contrat.getStatut() != StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE) {
            throw new KupangaBusinessException("Ce contrat ne peut plus être signé", HttpStatus.CONFLICT);
        }
        bienService.verifierDocumentModifiable(contrat.getBien(), contrat.getProprietaire(), contrat.getLocataire());

        contrat.setSignatureLocataire(signatureBase64);
        contrat.setDateSignatureLocataire(LocalDateTime.now());
        contrat.setStatut(StatutContrat.SIGNE);

        // Génère le PDF final avec les deux signatures
        String clePdf = contratPdfService.genererEtUploaderPdf(contrat);
        contrat.setClePdf(clePdf);

        // Invalide le token
        contrat.setTokenSignature(null);
        contrat.setTokenExpiration(null);
        // B6 : écriture immédiate : un conflit (409) est détecté avant les e-mails et notifications
        contratRepository.saveAndFlush(contrat);

        // Envoie les emails de confirmation aux deux parties
        emailService.envoyerConfirmationContratSigne(contrat);

        // Notifications de confirmation aux deux parties
        notificationService.saveAndSend(
                contrat.getLocataire(),
                NotificationType.CONTRAT_SIGNE,
                "Contrat signé avec succès",
                "Votre contrat pour le bien " + contrat.getAdresseBien()
                        + " a été signé par les deux parties.",
                null,
                contrat.getId()
        );
        notificationService.saveAndSend(
                contrat.getProprietaire(),
                NotificationType.CONTRAT_SIGNE,
                "Contrat signé",
                "Le locataire " + contrat.getLocataire().getFirstName() + " "
                        + contrat.getLocataire().getLastName()
                        + " vient de signer le contrat pour le bien "
                        + contrat.getAdresseBien() + ".",
                null,
                contrat.getId()
        );
    }

    /**
     * Trouver le contrat et son propriétaire.
     * @param contratId id du contrat
     * @param email email
     * @return le contrat.
     */
    // B7 : 404 / 410 et non 401, que le front traite comme une session expirée (retour à la connexion)
    private static KupangaBusinessException lienInvalide() {
        return new KupangaBusinessException("Lien de signature invalide", HttpStatus.NOT_FOUND);
    }

    private static KupangaBusinessException lienExpire() {
        return new KupangaBusinessException("Le lien de signature a expiré", HttpStatus.GONE);
    }

    private Contrat findAndVerify(Long contratId, String email) {
        Contrat contrat = contratRepository.findById(contratId)
                .orElseThrow(() -> new KupangaBusinessException("Aucun contrat trouvé " , HttpStatus.NOT_FOUND));
        if (!contrat.getProprietaire().getMail().equals(email)) {
            throw new KupangaBusinessException("Accès refusé : ce contrat ne vous appartient pas" , HttpStatus.FORBIDDEN);
        }
        return contrat;
    }
}

