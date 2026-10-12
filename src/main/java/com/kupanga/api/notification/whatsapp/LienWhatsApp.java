package com.kupanga.api.notification.whatsapp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * C7-C16 : construit un lien {@code wa.me}. Le numéro vient de {@code User.telephone} (E.164, contrôlé par
 * l'application et par un {@code CHECK} en base) ; il est revérifié ici pour ne jamais produire une URL arbitraire.
 */
public final class LienWhatsApp {

    private static final String BASE = "https://wa.me/";

    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    private LienWhatsApp() {
    }

    /**
     * @param telephone numéro E.164 du destinataire, ou {@code null} (WhatsApp proposera de choisir le contact)
     * @param texte     message prérempli
     */
    public static LienWhatsAppDTO creer(String telephone, String texte) {
        boolean numeroConnu = telephone != null && E164.matcher(telephone).matches();
        // URLEncoder encode l'espace en « + », que WhatsApp afficherait tel quel
        String texteEncode = URLEncoder.encode(texte, StandardCharsets.UTF_8).replace("+", "%20");
        String url = BASE + (numeroConnu ? telephone.substring(1) : "") + "?text=" + texteEncode;
        return new LienWhatsAppDTO(url, texte, numeroConnu);
    }
}
