package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
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

    private final EtatDesLieuxRepository edlRepository;
    private final EtatDesLieuxPdfService  edlPdfService;
    private final EmailService            emailService;
    private final EtatDesLieuxMapper      edlMapper;
    private final BienService             bienService;
    private final UserService             userService;

    // ─────────────────────────────────────────────────────────────────────────
    // Création
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void creerEtatDesLieux(EtatDesLieuxFormDTO dto, String emailProprietaire) {

        User proprietaire = userService.getUserByEmail(emailProprietaire);
        User locataire    = userService.getUserByEmail(dto.getEmailLocataire());
        Bien bien         = bienService.findWithAllProperties(dto.getBienId());

        EtatDesLieux edl = EtatDesLieux.builder()
                .bien(bien)
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
        String urlPdf = edlPdfService.genererEtUploaderPdf(saved);
        saved.setUrlPdf(urlPdf);
        edlRepository.save(saved);

        log.info("EDL {} créé pour le bien {}", saved.getId(), bien.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Signature propriétaire
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void signerProprietaire(Long edlId, String signatureBase64, String emailProprietaire) {

        EtatDesLieux edl = findAndVerifyProprietaire(edlId, emailProprietaire);

        edl.setSignatureProprietaire(signatureBase64);
        edl.setDateSignatureProprietaire(LocalDateTime.now());
        edl.setStatut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        String token = UUID.randomUUID().toString();
        edl.setTokenSignature(token);
        edl.setTokenExpiration(LocalDateTime.now().plusHours(72));

        String urlPdf = edlPdfService.genererEtUploaderPdf(edl);
        edl.setUrlPdf(urlPdf);
        edlRepository.save(edl);

        emailService.envoyerInvitationSignature(edl, token);

        log.info("EDL {} signé par le propriétaire, invitation envoyée à {}",
                edlId, edl.getLocataire().getMail());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Signature locataire
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void signerLocataire(String token, String signatureBase64) {

        EtatDesLieux edl = edlRepository.findByTokenSignature(token)
                .orElseThrow(() -> new KupangaBusinessException(
                        "Token invalide", HttpStatus.UNAUTHORIZED));

        if (LocalDateTime.now().isAfter(edl.getTokenExpiration())) {
            edl.setStatut(StatutEdl.EXPIRE);
            edlRepository.save(edl);
            throw new KupangaBusinessException(
                    "Le lien de signature a expiré", HttpStatus.UNAUTHORIZED);
        }

        if (edl.getStatut() != StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE) {
            throw new KupangaBusinessException(
                    "Cet état des lieux ne peut plus être signé — statut actuel : "
                            + edl.getStatut(), HttpStatus.BAD_REQUEST);
        }

        edl.setSignatureLocataire(signatureBase64);
        edl.setDateSignatureLocataire(LocalDateTime.now());
        edl.setStatut(StatutEdl.SIGNE);

        String urlPdf = edlPdfService.genererEtUploaderPdf(edl);
        edl.setUrlPdf(urlPdf);

        edl.setTokenSignature(null);
        edl.setTokenExpiration(null);
        edlRepository.save(edl);

        emailService.envoyerConfirmationEdlSigne(edl);

        log.info("EDL {} signé par les deux parties — statut : SIGNE", edl.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consultation par token
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public EtatDesLieuxDTO getEdlParToken(String token) {

        EtatDesLieux edl = edlRepository.findByTokenSignature(token)
                .orElseThrow(() -> new KupangaBusinessException(
                        "Token invalide", HttpStatus.UNAUTHORIZED));

        if (LocalDateTime.now().isAfter(edl.getTokenExpiration())) {
            edl.setStatut(StatutEdl.EXPIRE);
            edlRepository.save(edl);
            throw new KupangaBusinessException(
                    "Le lien de signature a expiré", HttpStatus.UNAUTHORIZED);
        }

        if (edl.getStatut() != StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE) {
            throw new KupangaBusinessException(
                    "Cet état des lieux n'est pas disponible à la signature — statut actuel : "
                            + edl.getStatut(), HttpStatus.BAD_REQUEST);
        }

        return edlMapper.toDTO(edl);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────────────

    private EtatDesLieux findAndVerifyProprietaire(Long edlId, String email) {
        EtatDesLieux edl = edlRepository.findWithAllRelations(edlId)
                .orElseThrow(() -> new KupangaBusinessException(
                        "État des lieux introuvable", HttpStatus.NOT_FOUND));
        if (!edl.getProprietaire().getMail().equals(email)) {
            throw new KupangaBusinessException(
                    "Accès non autorisé", HttpStatus.UNAUTHORIZED);
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
                        .typeElement(TypeElement.valueOf(eDto.getTypeElement()))
                        .etatElement(EtatElement.valueOf(eDto.getEtatElement()))
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
                .typeCompteur(TypeCompteur.valueOf(dto.getTypeCompteur()))
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