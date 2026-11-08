package com.kupanga.api.email.dto;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M1 : version texte d'un e-mail HTML (partie {@code text/plain} du message). Les boîtes qui n'affichent pas le
 * HTML la montrent, et un message sans texte brut est plus souvent classé en spam. Les liens restent lisibles :
 * « libellé : https://… ».
 */
public final class TexteBrut {

    private static final Pattern ENTETE = Pattern.compile("(?is)<head\\b.*?</head>|<style\\b.*?</style>|<script\\b.*?</script>");
    /** Texte d'aperçu caché (preheader) : déjà repris par le corps du message. */
    private static final Pattern CACHE = Pattern.compile("(?is)<div[^>]*display\\s*:\\s*none[^>]*>.*?</div>");
    private static final Pattern LIEN = Pattern.compile("(?is)<a\\b[^>]*\\bhref\\s*=\\s*\"([^\"]*)\"[^>]*>(.*?)</a>");
    private static final Pattern ALT_IMAGE = Pattern.compile("(?is)<img\\b[^>]*\\balt\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern BLOC =Pattern.compile("(?i)<\\s*(br|/p|/div|/tr|/h[1-6]|/li|/table)\\b[^>]*>");
    private static final Pattern BALISE = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern ESPACES = Pattern.compile("[ \\t\\x0B\\f\\u00A0]+");
    private static final Pattern LIGNES_VIDES = Pattern.compile("\\n\\s*\\n\\s*\\n+");

    private TexteBrut() {
    }

    public static String depuisHtml(String html) {
        if (html == null) return "";
        String texte = ENTETE.matcher(html).replaceAll("");
        texte = CACHE.matcher(texte).replaceAll("");
        texte = remplacerLiens(texte);
        texte = BLOC.matcher(texte).replaceAll("\n");
        texte = BALISE.matcher(texte).replaceAll("");
        texte = HtmlUtils.htmlUnescape(texte);
        texte = ESPACES.matcher(texte).replaceAll(" ");
        StringBuilder lignes = new StringBuilder();
        for (String ligne : texte.split("\n")) {
            lignes.append(ligne.strip()).append('\n');
        }
        return LIGNES_VIDES.matcher(lignes.toString()).replaceAll("\n\n").strip();
    }

    private static String remplacerLiens(String html) {
        Matcher m = LIEN.matcher(html);
        StringBuilder sortie = new StringBuilder();
        while (m.find()) {
            String libelle = BALISE.matcher(m.group(2)).replaceAll("").strip();
            String url = m.group(1).strip();
            String remplacement;
            if (libelle.isEmpty()) {
                // Lien sur une image seule (le logo en tête d'e-mail) : son texte de remplacement, sans l'adresse.
                Matcher alt = ALT_IMAGE.matcher(m.group(2));
                remplacement = alt.find() && !alt.group(1).isBlank() ? alt.group(1).strip() : url;
            } else {
                remplacement = libelle.equals(url) ? url : libelle + " : " + url;
            }
            m.appendReplacement(sortie, Matcher.quoteReplacement(remplacement));
        }
        m.appendTail(sortie);
        return sortie.toString();
    }
}
