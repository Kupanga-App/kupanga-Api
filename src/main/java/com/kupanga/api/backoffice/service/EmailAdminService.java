package com.kupanga.api.backoffice.service;

import com.kupanga.api.backoffice.dto.EmailAdminDTO;
import com.kupanga.api.backoffice.dto.EmailAdminPageDTO;
import com.kupanga.api.backoffice.dto.EmailAdminSearchDTO;
import com.kupanga.api.config.ApresCommit;
import com.kupanga.api.email.file.EmailEnFile;
import com.kupanga.api.email.file.EmailEnFileRepository;
import com.kupanga.api.email.file.EnvoyeurEmails;
import com.kupanga.api.email.file.StatutEmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** M4 : suivi de la file d'e-mails depuis le back-office (en attente, échecs, envoyés) et renvoi manuel. */
@Slf4j
@Service
public class EmailAdminService {

    private final EmailEnFileRepository depot;
    private final EnvoyeurEmails envoyeur;
    private final Clock horloge;

    @Autowired
    public EmailAdminService(EmailEnFileRepository depot, @Lazy EnvoyeurEmails envoyeur) {
        this(depot, envoyeur, Clock.systemUTC());
    }

    EmailAdminService(EmailEnFileRepository depot, EnvoyeurEmails envoyeur, Clock horloge) {
        this.depot = depot;
        this.envoyeur = envoyeur;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    public EmailAdminPageDTO rechercher(EmailAdminSearchDTO dto) {
        Pageable pageable = PageRequest.of(dto.page(), dto.size(), Sort.by(Sort.Direction.DESC, "id"));
        Page<EmailEnFile> page;
        if (dto.statut() != null && dto.type() != null) {
            page = depot.findByStatutAndType(dto.statut(), dto.type(), pageable);
        } else if (dto.statut() != null) {
            page = depot.findByStatut(dto.statut(), pageable);
        } else if (dto.type() != null) {
            page = depot.findByType(dto.type(), pageable);
        } else {
            page = depot.findAll(pageable);
        }
        Instant maintenant = horloge.instant();
        return EmailAdminPageDTO.from(page.map(email -> EmailAdminDTO.from(email, maintenant)));
    }

    /** Tableau de bord : e-mails en échec définitif encore conservés. */
    @Transactional(readOnly = true)
    public long compterEchecs() {
        return depot.countByStatut(StatutEmail.ECHEC_DEFINITIF);
    }

    /** Tableau de bord : e-mails en attente d'un (nouvel) essai. */
    @Transactional(readOnly = true)
    public long compterEnAttente() {
        return depot.countByStatut(StatutEmail.A_ENVOYER) + depot.countByStatut(StatutEmail.EN_COURS);
    }

    /**
     * Remet en file un e-mail en échec définitif (ex. après correction de la configuration SMTP ou de l'expéditeur) :
     * nouvelle série d'essais, envoi dans les secondes qui suivent.
     */
    @Transactional
    public ResultatRenvoiEmail renvoyer(Long id) {
        EmailEnFile email = depot.findById(id).orElse(null);
        if (email == null) {
            return ResultatRenvoiEmail.INTROUVABLE;
        }
        Instant maintenant = horloge.instant();
        if (email.getStatut() != StatutEmail.ECHEC_DEFINITIF) {
            return ResultatRenvoiEmail.PAS_EN_ECHEC;
        }
        if (email.isContenuEfface() || email.getHtml() == null) {
            return ResultatRenvoiEmail.CONTENU_EFFACE;
        }
        if (email.getExpireLe() != null && !maintenant.isBefore(email.getExpireLe())) {
            return ResultatRenvoiEmail.LIEN_EXPIRE;
        }
        email.setStatut(StatutEmail.A_ENVOYER);
        email.setTentatives(0);
        email.setProchainEssai(maintenant);
        email.setVerrouJusqua(null);
        email.setDerniereErreur(null);
        depot.save(email);
        log.info("E-mail {} ({}) remis en file depuis le back-office", id, email.getType());
        ApresCommit.executer(envoyeur::reveiller);
        return ResultatRenvoiEmail.REMIS_EN_FILE;
    }
}
