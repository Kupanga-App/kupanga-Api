package com.kupanga.api.notification.whatsapp;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.juridiction.CanalNotification;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class LienWhatsAppServiceImpl implements LienWhatsAppService {

    static final String TEXTE_INVITATION_CONTRAT = """
            Bonjour %s, j'ai signé le bail du logement situé %s. \
            Il vous reste à le signer sur Kupanga : connectez-vous (%s) \
            puis ouvrez l'invitation reçue dans vos notifications ou par e-mail.""";

    static final String TEXTE_INVITATION_EDL = """
            Bonjour %s, j'ai signé l'état des lieux %s du logement situé %s. \
            Il vous reste à le signer sur Kupanga : connectez-vous (%s) \
            puis ouvrez l'invitation reçue dans vos notifications ou par e-mail.""";

    static final String TEXTE_RAPPEL_LOYER = """
            Bonjour %s, petit rappel : le loyer de %s %d du logement situé %s (%s) \
            n'est pas encore réglé. Merci de me prévenir dès que le paiement est fait.""";

    private static final Set<StatutQuittance> NON_PAYEES =
            Set.of(StatutQuittance.EN_ATTENTE, StatutQuittance.EN_RETARD, StatutQuittance.IMPAYEE);

    private final ContratRepository contratRepository;
    private final EtatDesLieuxRepository edlRepository;
    private final QuittanceRepository quittanceRepository;
    private final BienService bienService;
    private final JuridictionRegistry juridictionRegistry;
    private final String urlConnexion;

    public LienWhatsAppServiceImpl(
            ContratRepository contratRepository,
            EtatDesLieuxRepository edlRepository,
            QuittanceRepository quittanceRepository,
            BienService bienService,
            JuridictionRegistry juridictionRegistry,
            @Value("${app.url-login}") String urlConnexion
    ) {
        this.contratRepository = contratRepository;
        this.edlRepository = edlRepository;
        this.quittanceRepository = quittanceRepository;
        this.bienService = bienService;
        this.juridictionRegistry = juridictionRegistry;
        this.urlConnexion = urlConnexion;
    }

    @Override
    @Transactional(readOnly = true)
    public LienWhatsAppDTO invitationContrat(Long contratId, String emailProprietaire) {
        Contrat contrat = contratRepository.findById(contratId)
                .orElseThrow(() -> new KupangaBusinessException("Aucun contrat trouvé ", HttpStatus.NOT_FOUND));
        verifierProprietaire(contrat.getProprietaire(), emailProprietaire);
        verifierCanal(contrat.getPays());
        bienService.verifierDocumentModifiable(contrat.getBien(), contrat.getProprietaire(), contrat.getLocataire());
        if (contrat.getStatut() != StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE || expire(contrat.getTokenExpiration())) {
            throw new KupangaBusinessException(
                    "Ce contrat n'attend pas la signature du locataire : signez-le pour envoyer une nouvelle invitation",
                    HttpStatus.CONFLICT);
        }
        User locataire = contrat.getLocataire();
        return LienWhatsApp.creer(locataire.getTelephone(), TEXTE_INVITATION_CONTRAT.formatted(
                locataire.getFirstName(), contrat.getAdresseBien(), urlConnexion));
    }

    @Override
    @Transactional(readOnly = true)
    public LienWhatsAppDTO invitationEtatDesLieux(Long edlId, String emailProprietaire) {
        EtatDesLieux edl = edlRepository.findById(edlId)
                .orElseThrow(() -> new KupangaBusinessException("État des lieux introuvable", HttpStatus.NOT_FOUND));
        verifierProprietaire(edl.getProprietaire(), emailProprietaire);
        verifierCanal(edl.getPays() != null ? edl.getPays() : edl.getBien().getPays());
        bienService.verifierDocumentModifiable(edl.getBien(), edl.getProprietaire(), edl.getLocataire());
        if (edl.getStatut() != StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE || expire(edl.getTokenExpiration())) {
            throw new KupangaBusinessException(
                    "Cet état des lieux n'attend pas la signature du locataire : signez-le pour envoyer une nouvelle invitation",
                    HttpStatus.CONFLICT);
        }
        User locataire = edl.getLocataire();
        String type = edl.getType() == TypeEtat.SORTIE ? "de sortie" : "d'entrée";
        return LienWhatsApp.creer(locataire.getTelephone(), TEXTE_INVITATION_EDL.formatted(
                locataire.getFirstName(), type, edl.getBien().adresseComplete(), urlConnexion));
    }

    @Override
    @Transactional(readOnly = true)
    public LienWhatsAppDTO rappelLoyer(Long quittanceId, String emailProprietaire) {
        Quittance quittance = quittanceRepository.findById(quittanceId)
                .orElseThrow(() -> new KupangaBusinessException("Quittance introuvable", HttpStatus.NOT_FOUND));
        verifierProprietaire(quittance.getProprietaire(), emailProprietaire);
        verifierCanal(quittance.getPays());
        bienService.verifierDocumentModifiable(quittance.getBien(), quittance.getProprietaire(), quittance.getLocataire());
        if (!NON_PAYEES.contains(quittance.getStatut())) {
            throw new KupangaBusinessException("Ce loyer est déjà payé", HttpStatus.CONFLICT);
        }
        User locataire = quittance.getLocataire();
        String montant = juridictionRegistry.formatMontant(quittance.getPays(), quittance.getDevise())
                .f(quittance.getMontantTotal());
        return LienWhatsApp.creer(locataire.getTelephone(), TEXTE_RAPPEL_LOYER.formatted(
                locataire.getFirstName(), quittance.getMois(), quittance.getAnnee(),
                quittance.getBien().adresseComplete(), montant));
    }

    private static void verifierProprietaire(User proprietaire, String email) {
        if (proprietaire == null || !proprietaire.getMail().equals(email)) {
            throw new KupangaBusinessException("Accès non autorisé", HttpStatus.FORBIDDEN);
        }
    }

    /** Canal du pays figé sur le document (J3/J5) ; un pays sans profil n'a aucun canal. */
    private void verifierCanal(Pays pays) {
        boolean propose = juridictionRegistry.estPrisEnCharge(pays)
                && juridictionRegistry.profil(pays).notification().canaux().contains(CanalNotification.WHATSAPP_LIEN);
        if (!propose) {
            throw new KupangaBusinessException(
                    "L'envoi par WhatsApp n'est pas proposé pour les logements de ce pays", HttpStatus.CONFLICT);
        }
    }

    private static boolean expire(LocalDateTime expiration) {
        return expiration == null || LocalDateTime.now().isAfter(expiration);
    }
}
