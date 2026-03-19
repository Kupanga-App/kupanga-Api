package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.QuittanceFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.QuittanceMapper;
import com.kupanga.api.immobilier.pdf.QuittancePdfService;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.QuittanceService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QuittanceServiceImpl implements QuittanceService {

    private final QuittanceRepository quittanceRepository;
    private final QuittancePdfService  quittancePdfService;
    private final QuittanceMapper      quittanceMapper;
    private final BienService          bienService;
    private final UserService          userService;
    private final ContratRepository    contratRepository;
    private final EmailService         emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // Création
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void creerQuittance(QuittanceFormDTO dto, String emailProprietaire) {

        User proprietaire = userService.getUserByEmail(emailProprietaire);
        User locataire    = userService.getUserByEmail(dto.getEmailLocataire());
        Bien bien         = bienService.findWithAllProperties(dto.getBienId());

        // Vérifie qu'une quittance n'existe pas déjà pour ce bien / mois / année
        quittanceRepository.findByBienIdAndMoisAndAnnee(dto.getBienId(), dto.getMois(), dto.getAnnee())
                .ifPresent(q -> { throw new KupangaBusinessException(
                        "Une quittance existe déjà pour ce bien en "
                                + dto.getMois() + "/" + dto.getAnnee(),
                        HttpStatus.CONFLICT); });

        // Si un contrat est fourni, on récupère loyer et charges depuis le contrat
        Double loyer   = dto.getLoyerMensuel();
        Double charges = dto.getChargesMensuelles();
        Contrat contrat = null;

        if (dto.getContratId() != null) {
            contrat = contratRepository.findById(dto.getContratId())
                    .orElseThrow(() -> new KupangaBusinessException(
                            "Contrat introuvable : " + dto.getContratId(), HttpStatus.NOT_FOUND));
            loyer   = contrat.getLoyerMensuel();
            charges = contrat.getChargesMensuelles();
        }

        if (loyer == null || charges == null) {
            throw new KupangaBusinessException(
                    "Loyer et charges obligatoires si aucun contrat n'est fourni",
                    HttpStatus.BAD_REQUEST);
        }

        Quittance quittance = Quittance.builder()
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .contrat(contrat)
                .mois(dto.getMois())
                .annee(dto.getAnnee())
                .loyerMensuel(loyer)
                .chargesMensuelles(charges)
                .montantTotal(loyer + charges)
                .dateEcheance(dto.getDateEcheance())
                .datePaiement(dto.getDatePaiement())
                .statut(dto.getDatePaiement() != null
                        ? StatutQuittance.PAYEE
                        : StatutQuittance.EN_ATTENTE)
                .build();

        Quittance saved = quittanceRepository.save(quittance);

        // Génère le PDF
        String urlPdf = quittancePdfService.genererEtUploaderPdf(saved);
        saved.setUrlPdf(urlPdf);
        quittanceRepository.save(saved);

        log.info("Quittance {} créée pour le bien {} — {}/{}",
                saved.getId(), bien.getId(), dto.getMois(), dto.getAnnee());

    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marquer payée
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void marquerPayee(Long quittanceId, String emailProprietaire) {

        Quittance quittance = findAndVerifyProprietaire(quittanceId, emailProprietaire);

        if (quittance.getStatut() == StatutQuittance.PAYEE) {
            throw new KupangaBusinessException(
                    "Cette quittance est déjà marquée comme payée", HttpStatus.BAD_REQUEST);
        }

        quittance.setStatut(StatutQuittance.PAYEE);
        quittance.setDatePaiement(LocalDate.now());

        // Régénère le PDF avec la date de paiement
        String urlPdf = quittancePdfService.genererEtUploaderPdf(quittance);
        quittance.setUrlPdf(urlPdf);
        quittanceRepository.save(quittance);

        // Envoie la quittance par email au locataire
        emailService.envoyerQuittance(quittance);

        log.info("Quittance {} marquée payée — email envoyé à {}",
                quittanceId, quittance.getLocataire().getMail());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lecture — vue propriétaire
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<QuittanceDTO> getQuittancesParBien(Long bienId, String emailProprietaire) {
        User proprietaire = userService.getUserByEmail(emailProprietaire);
        return quittanceRepository.findByBienId(bienId)
                .stream()
                .filter(q -> q.getProprietaire().getId().equals(proprietaire.getId()))
                .map(quittanceMapper::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lecture — vue locataire
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<QuittanceDTO> getQuittancesParLocataire(String emailLocataire) {
        User locataire = userService.getUserByEmail(emailLocataire);
        return quittanceRepository.findByProprietaireId(locataire.getId())
                .stream()
                .map(quittanceMapper::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lecture — par id
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public QuittanceDTO getQuittanceById(Long quittanceId, String emailUtilisateur) {
        Quittance quittance = quittanceRepository.findWithAllRelations(quittanceId)
                .orElseThrow(() -> new KupangaBusinessException(
                        "Quittance introuvable : " + quittanceId, HttpStatus.NOT_FOUND));

        // Accessible par le propriétaire OU le locataire
        boolean estProprietaire = quittance.getProprietaire().getMail().equals(emailUtilisateur);
        boolean estLocataire    = quittance.getLocataire().getMail().equals(emailUtilisateur);

        if (!estProprietaire && !estLocataire) {
            throw new KupangaBusinessException("Accès non autorisé", HttpStatus.UNAUTHORIZED);
        }

        return quittanceMapper.toDTO(quittance);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper privé
    // ─────────────────────────────────────────────────────────────────────────

    private Quittance findAndVerifyProprietaire(Long quittanceId, String email) {
        Quittance quittance = quittanceRepository.findWithAllRelations(quittanceId)
                .orElseThrow(() -> new KupangaBusinessException(
                        "Quittance introuvable : " + quittanceId, HttpStatus.NOT_FOUND));
        if (!quittance.getProprietaire().getMail().equals(email)) {
            throw new KupangaBusinessException("Accès non autorisé", HttpStatus.UNAUTHORIZED);
        }
        return quittance;
    }
}