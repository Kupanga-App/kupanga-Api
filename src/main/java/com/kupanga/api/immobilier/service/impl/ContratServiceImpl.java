package com.kupanga.api.immobilier.service.impl;

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

    private final ContratRepository contratRepository;
    private final ContratPdfService contratPdfService;
    private final EmailService emailService;
    private final UserService userService;
    private final BienService bienService;
    private final ContratMapper contratMapper;

    @Override
    public void creerContrat(ContratFormDTO dto, String emailProprietaire) {

        User proprietaire = userService.getUserByEmail(emailProprietaire);

        User locataire = userService.getUserByEmail(dto.getEmailLocataire());

        Bien bien = bienService.findWithAllProperties(dto.getBienId());

        Contrat contrat = Contrat.builder()
                .bien(bien)
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
        String urlPdf = contratPdfService.genererEtUploaderPdf(contrat);
        contrat.setUrlPdf(urlPdf);
        contratRepository.save(contrat);

    }

    public ContratDTO getContratParToken(String token) {

        Contrat contrat = contratRepository.findByTokenSignature(token)
                .orElseThrow(() -> new KupangaBusinessException("Token Invalide" , HttpStatus.UNAUTHORIZED));

        // Vérifie si le token est expiré
        if (LocalDateTime.now().isAfter(contrat.getTokenExpiration())) {
            contrat.setStatut(StatutContrat.EXPIRE);
            contratRepository.save(contrat);
            throw new KupangaBusinessException("Le lien de la signature a éxpiré" , HttpStatus.UNAUTHORIZED);
        }

        // Vérifie que le contrat est bien en attente de signature locataire
        if (contrat.getStatut() != StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE) {
            throw new IllegalStateException(
                    "Ce contrat ne peut plus être signé — statut actuel : " + contrat.getStatut()
            );
        }

        return contratMapper.toDTO(contrat);
    }

    @Override
    public void signerProprietaire(Long contratId, String signatureBase64,
                                   String emailProprietaire) {

        Contrat contrat = findAndVerify(contratId, emailProprietaire);

        contrat.setSignatureProprietaire(signatureBase64);
        contrat.setDateSignatureProprietaire(LocalDateTime.now());
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        // Génère un token unique pour le locataire
        String token = UUID.randomUUID().toString();
        contrat.setTokenSignature(token);
        contrat.setTokenExpiration(LocalDateTime.now().plusHours(72));

        // Regénère le PDF avec la signature du proprio
        String urlPdf = contratPdfService.genererEtUploaderPdf(contrat);
        contrat.setUrlPdf(urlPdf);
        contratRepository.save(contrat);

        // Envoie l'email au locataire
        emailService.envoyerInvitationSignature(contrat, token);
    }

    @Override
    public void signerLocataire(String token, String signatureBase64) {

        Contrat contrat = contratRepository.findByTokenSignature(token)
                .orElseThrow(() -> new KupangaBusinessException("Token Invalide" , HttpStatus.UNAUTHORIZED));

        // Vérifie l'expiration
        if (LocalDateTime.now().isAfter(contrat.getTokenExpiration())) {
            contrat.setStatut(StatutContrat.EXPIRE);
            contratRepository.save(contrat);
            throw new KupangaBusinessException("Le lien de la signature a éxpiré" , HttpStatus.UNAUTHORIZED);
        }

        contrat.setSignatureLocataire(signatureBase64);
        contrat.setDateSignatureLocataire(LocalDateTime.now());
        contrat.setStatut(StatutContrat.SIGNE);

        // Génère le PDF final avec les deux signatures
        String urlPdf = contratPdfService.genererEtUploaderPdf(contrat);
        contrat.setUrlPdf(urlPdf);

        // Invalide le token
        contrat.setTokenSignature(null);
        contrat.setTokenExpiration(null);
        contratRepository.save(contrat);

        // Envoie les emails de confirmation aux deux parties
        emailService.envoyerConfirmationContratSigne(contrat);
    }

    /**
     * Trouver le contrat et son propriétaire.
     * @param contratId id du contrat
     * @param email email
     * @return le contrat.
     */
    private Contrat findAndVerify(Long contratId, String email) {
        Contrat contrat = contratRepository.findById(contratId)
                .orElseThrow(() -> new KupangaBusinessException("Aucun contrat trouvé " , HttpStatus.NOT_FOUND));
        if (!contrat.getProprietaire().getMail().equals(email)) {
            throw new KupangaBusinessException(" Email du propriétaire incorrect " , HttpStatus.UNAUTHORIZED);
        }
        return contrat;
    }
}

