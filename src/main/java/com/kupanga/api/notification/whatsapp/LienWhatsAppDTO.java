package com.kupanga.api.notification.whatsapp;

/**
 * C7-C16 : lien « click-to-chat » WhatsApp préparé pour le propriétaire, qui l'ouvre depuis son téléphone.
 * Aucune API WhatsApp n'est appelée : le message part du numéro du propriétaire, connu du locataire.
 *
 * @param url         {@code https://wa.me/<numéro sans +>?text=…}, ou {@code https://wa.me/?text=…} (choix du contact
 *                    dans WhatsApp) si le locataire n'a pas renseigné de numéro
 * @param texte       message prérempli, modifiable dans WhatsApp avant l'envoi
 * @param numeroConnu vrai si le lien vise directement le numéro du locataire
 */
public record LienWhatsAppDTO(
        String url,
        String texte,
        boolean numeroConnu
) {}
