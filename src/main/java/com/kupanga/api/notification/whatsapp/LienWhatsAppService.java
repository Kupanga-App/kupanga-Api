package com.kupanga.api.notification.whatsapp;

/**
 * C7-C16 : messages WhatsApp préremplis que le propriétaire envoie lui-même au locataire (lien {@code wa.me}).
 * Proposés seulement si le profil du pays du document a le canal {@code WHATSAPP_LIEN} (sinon 409).
 * Les invitations à signer ne contiennent <b>jamais</b> le jeton de signature : le propriétaire, qui détient le
 * message, ne doit pas pouvoir signer à la place du locataire ; celui-ci se connecte et signe depuis
 * l'invitation reçue (notification de l'application ou e-mail).
 */
public interface LienWhatsAppService {

    /** Invitation à signer un bail en attente de la signature du locataire (404, 403, 409). */
    LienWhatsAppDTO invitationContrat(Long contratId, String emailProprietaire);

    /** Invitation à signer un état des lieux en attente de la signature du locataire (404, 403, 409). */
    LienWhatsAppDTO invitationEtatDesLieux(Long edlId, String emailProprietaire);

    /** Rappel de loyer pour une quittance non payée (404, 403, 409). */
    LienWhatsAppDTO rappelLoyer(Long quittanceId, String emailProprietaire);
}
