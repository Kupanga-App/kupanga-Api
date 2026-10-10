package com.kupanga.api.juridiction;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.entity.TypeBien;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Point d'entrée unique vers les juridictions (CLAUDE.md §4bis) : profil (données YAML) et règle (logique)
 * du pays d'un bien. Les services passent par ici, jamais par un {@code if (pays == ...)}.
 * <p>
 * La configuration est contrôlée à la construction : une incohérence (pays sans règle, règle sans profil,
 * devise par défaut hors liste, champ inconnu…) empêche l'application de démarrer.
 */
@Component
public class JuridictionRegistry {

    /** Noms des champs du formulaire de bien, seuls admis dans champs-obligatoires et champs-masques. */
    private static final Set<String> CHAMPS_DU_BIEN = Arrays.stream(BienFormDTO.class.getDeclaredFields())
            .filter(champ -> !Modifier.isStatic(champ.getModifiers()))
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());

    /** J5 : version d'un modèle de documents : code du pays en minuscules, « -v », numéro (ex. {@code cd-v1}). */
    private static final Pattern VERSION_MODELE = Pattern.compile("^([a-z]{2})-v[0-9]{1,3}$");

    private final Map<Pays, ProfilJuridiction> profils;
    private final Map<Pays, RegleJuridiction> regles;
    private final Map<Devise, PlafondsMontants> plafonds;

    public JuridictionRegistry(JuridictionProperties properties, List<RegleJuridiction> regles) {
        if (properties.juridictions().isEmpty()) {
            throw new IllegalStateException("kupanga.juridictions : aucun pays configuré");
        }
        this.profils = Collections.unmodifiableMap(new EnumMap<>(properties.juridictions()));
        this.regles = Collections.unmodifiableMap(indexer(regles));
        this.plafonds = properties.plafonds();
        verifier();
    }

    /**
     * @return le profil du pays
     * @throws KupangaBusinessException 400 si le pays n'est pas encore pris en charge
     */
    public ProfilJuridiction profil(Pays pays) {
        ProfilJuridiction profil = pays == null ? null : profils.get(pays);
        if (profil == null) {
            throw nonPrisEnCharge(pays);
        }
        return profil;
    }

    /**
     * @return la règle du pays
     * @throws KupangaBusinessException 400 si le pays n'est pas encore pris en charge
     */
    public RegleJuridiction regle(Pays pays) {
        RegleJuridiction regle = pays == null ? null : regles.get(pays);
        if (regle == null) {
            throw nonPrisEnCharge(pays);
        }
        return regle;
    }

    /**
     * J3 : devise d'un bien. Sans devise demandée, celle du profil par défaut.
     * @throws KupangaBusinessException 400 si la devise n'est pas acceptée dans ce pays
     */
    public Devise devisePour(Pays pays, Devise demandee) {
        ProfilJuridiction profil = profil(pays);
        if (demandee == null) {
            return profil.deviseDefaut();
        }
        if (!profil.devises().contains(demandee)) {
            throw new KupangaBusinessException("La devise " + demandee + " n'est pas acceptée pour un bien situé en "
                    + pays.getLibelle() + " (devises possibles : " + profil.devises() + ")", HttpStatus.BAD_REQUEST);
        }
        return demandee;
    }

    /**
     * C5 : contrôle les montants par rapport aux plafonds de la devise ; un montant {@code null} n'est pas contrôlé.
     * @throws KupangaBusinessException 400 au premier montant qui dépasse son plafond
     */
    public void verifierMontants(Devise devise, BigDecimal loyerMensuel, BigDecimal chargesMensuelles,
                                 BigDecimal depotGarantie) {
        PlafondsMontants plafond = plafonds.get(devise);
        if (plafond == null) {
            throw new KupangaBusinessException("Devise non prise en charge : " + devise, HttpStatus.BAD_REQUEST);
        }
        verifierPlafond("Le loyer mensuel", loyerMensuel, plafond.loyerMensuel(), devise);
        verifierPlafond("Les charges mensuelles", chargesMensuelles, plafond.chargesMensuelles(), devise);
        verifierPlafond("Le dépôt de garantie", depotGarantie, plafond.depotGarantie(), devise);
    }

    private static void verifierPlafond(String libelle, BigDecimal montant, BigDecimal maximum, Devise devise) {
        if (montant != null && montant.compareTo(maximum) > 0) {
            throw new KupangaBusinessException(libelle + " dépasse le plafond de "
                    + maximum.toPlainString() + " " + devise, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * J4 : contrôle d'un formulaire de bien selon le profil de son pays (C1) : champs obligatoires renseignés,
     * champs masqués absents, type de bien proposé, devise acceptée et, à la création, montants sous les plafonds
     * de la devise (C5) ; puis règles propres au pays ({@link RegleJuridiction#validerBien}).
     * <p>
     * Seules les propriétés présentes dans le formulaire sont contrôlées ({@code BienFormDTO} à la création,
     * {@code BienUpdateDTO} à la modification, sans adresse). À la modification, un champ obligatoire absent
     * signifie « inchangé » : il n'est pas exigé.
     *
     * @return les violations, une par champ fautif, dans l'ordre (vide si le pays n'est pas pris en charge :
     *         ce refus est fait ailleurs)
     */
    public List<ViolationChamp> controlerBien(Pays pays, Object formulaire, boolean creation) {
        if (formulaire == null || !estPrisEnCharge(pays)) {
            return List.of();
        }
        ProfilJuridiction profil = profils.get(pays);
        BeanWrapper valeurs = new BeanWrapperImpl(formulaire);
        String lieu = "un bien situé en " + pays.getLibelle();
        List<ViolationChamp> violations = new ArrayList<>();

        if (creation) {
            profil.champsObligatoires().stream().sorted()
                    .filter(champ -> valeurs.isReadableProperty(champ) && !renseigne(valeurs.getPropertyValue(champ)))
                    .forEach(champ -> violations.add(new ViolationChamp(champ, "Ce champ est obligatoire pour " + lieu)));
        }
        profil.champsMasques().stream().sorted()
                .filter(champ -> valeurs.isReadableProperty(champ) && renseigne(valeurs.getPropertyValue(champ)))
                .forEach(champ -> violations.add(new ViolationChamp(champ, "Ce champ ne s'applique pas à " + lieu)));

        if (valeurs.isReadableProperty("typeBien")
                && valeurs.getPropertyValue("typeBien") instanceof TypeBien type
                && !profil.typesBien().contains(type)) {
            violations.add(new ViolationChamp("typeBien", "Ce type de bien n'est pas proposé pour " + lieu));
        }

        Devise devise = valeurs.isReadableProperty("devise") ? (Devise) valeurs.getPropertyValue("devise") : null;
        if (devise != null && !profil.devises().contains(devise)) {
            violations.add(new ViolationChamp("devise", "La devise " + devise + " n'est pas acceptée pour " + lieu
                    + " (devises possibles : " + profil.devises() + ")"));
        } else if (creation) {
            controlerPlafonds(devise != null ? devise : profil.deviseDefaut(), valeurs, violations);
        }

        violations.addAll(regles.get(pays).validerBien(formulaire));
        return violations;
    }

    /**
     * J4 : comme {@link #controlerBien}, mais lève une 400 avec la première violation (défense en profondeur
     * dans les services, la contrainte {@code @ValideSelonJuridiction} renvoyant déjà le détail par champ).
     */
    public void verifierBien(Pays pays, Object formulaire, boolean creation) {
        List<ViolationChamp> violations = controlerBien(pays, formulaire, creation);
        if (!violations.isEmpty()) {
            ViolationChamp premiere = violations.get(0);
            throw new KupangaBusinessException(premiere.champ() + " : " + premiere.message(), HttpStatus.BAD_REQUEST);
        }
    }

    private void controlerPlafonds(Devise devise, BeanWrapper valeurs, List<ViolationChamp> violations) {
        PlafondsMontants plafond = plafonds.get(devise);
        if (plafond == null) {
            return;
        }
        Map<String, BigDecimal> maximums = new LinkedHashMap<>();
        maximums.put("loyerMensuel", plafond.loyerMensuel());
        maximums.put("chargesMensuelles", plafond.chargesMensuelles());
        maximums.put("depotGarantie", plafond.depotGarantie());
        maximums.forEach((champ, maximum) -> {
            if (valeurs.isReadableProperty(champ) && valeurs.getPropertyValue(champ) instanceof BigDecimal montant
                    && montant.compareTo(maximum) > 0) {
                violations.add(new ViolationChamp(champ,
                        "Le montant dépasse le plafond de " + maximum.toPlainString() + " " + devise));
            }
        });
    }

    /** Valeur saisie : non nulle et, pour un texte, non vide. */
    private static boolean renseigne(Object valeur) {
        return valeur != null && !(valeur instanceof String texte && texte.isBlank());
    }

    /**
     * J5 : nom Thymeleaf du modèle d'un document, d'après la version figée sur le document (C9) : un bail régénéré
     * après signature garde le modèle avec lequel il a été créé, même si le profil du pays est passé à une version
     * plus récente. Sans version (document antérieur), celle du profil courant.
     * <p>
     * La version vient de la base : elle est vérifiée (format et pays) avant de construire le chemin, qui ne peut
     * donc désigner qu'un modèle de {@code templates/documents/<pays>/}.
     *
     * @return ex. {@code documents/cd/cd-v1/contrat}
     * @throws IllegalStateException si la version est invalide ou ne correspond pas au pays du document
     */
    public String gabarit(TypeDocument type, Pays pays, String modeleVersion) {
        String version = modeleVersion != null ? modeleVersion : profil(pays).modeleDocuments();
        var correspondance = VERSION_MODELE.matcher(version);
        if (pays == null || !correspondance.matches() || !correspondance.group(1).equals(pays.codeMinuscule())) {
            throw new IllegalStateException("Version de modèle de document invalide pour " + pays + " : " + version);
        }
        return cheminGabarit(type, pays, version);
    }

    private static String cheminGabarit(TypeDocument type, Pays pays, String version) {
        return "documents/" + pays.codeMinuscule() + "/" + version + "/" + type.getFichier();
    }

    /** J3 : formateur des montants d'un document (langue du pays, devise figée). */
    public FormatMontant formatMontant(Pays pays, Devise devise) {
        return new FormatMontant(profil(pays).locale(), devise);
    }

    public boolean estPrisEnCharge(Pays pays) {
        return pays != null && profils.containsKey(pays);
    }

    /** Pays pris en charge, dans l'ordre de l'enum. */
    public Set<Pays> paysPrisEnCharge() {
        return Collections.unmodifiableSet(profils.keySet());
    }

    private static KupangaBusinessException nonPrisEnCharge(Pays pays) {
        String nom = pays == null ? "ce pays" : pays.getLibelle();
        return new KupangaBusinessException(
                "Les biens situés en " + nom + " ne sont pas encore pris en charge", HttpStatus.BAD_REQUEST);
    }

    private static Map<Pays, RegleJuridiction> indexer(List<RegleJuridiction> regles) {
        Map<Pays, RegleJuridiction> parPays = new EnumMap<>(Pays.class);
        for (RegleJuridiction regle : regles) {
            RegleJuridiction autre = parPays.put(regle.pays(), regle);
            if (autre != null) {
                throw new IllegalStateException("Plusieurs RegleJuridiction pour " + regle.pays() + " : "
                        + autre.getClass().getSimpleName() + ", " + regle.getClass().getSimpleName());
            }
        }
        return parPays;
    }

    private void verifier() {
        List<String> erreurs = new ArrayList<>();
        profils.forEach((pays, profil) -> verifierProfil(pays, profil, erreurs));
        regles.keySet().stream()
                .filter(pays -> !profils.containsKey(pays))
                .forEach(pays -> erreurs.add(pays + " : RegleJuridiction sans profil kupanga.juridictions." + pays));
        profils.values().stream()
                .flatMap(profil -> profil.devises().stream())
                .distinct()
                .sorted()
                .forEach(devise -> verifierPlafonds(devise, erreurs));
        if (!erreurs.isEmpty()) {
            throw new IllegalStateException("Configuration des juridictions invalide :\n - " + String.join("\n - ", erreurs));
        }
    }

    private void verifierProfil(Pays pays, ProfilJuridiction profil, List<String> erreurs) {
        String prefixe = "kupanga.juridictions." + pays;
        if (!regles.containsKey(pays)) {
            erreurs.add(prefixe + " : aucune RegleJuridiction pour ce pays");
        }
        if (profil.locale() == null) erreurs.add(prefixe + ".locale manquante");
        if (profil.fuseau() == null) erreurs.add(prefixe + ".fuseau manquant");
        if (profil.devises().isEmpty()) erreurs.add(prefixe + ".devises vide");
        if (profil.deviseDefaut() == null || !profil.devises().contains(profil.deviseDefaut())) {
            erreurs.add(prefixe + ".devise-defaut doit faire partie de devises");
        }
        if (profil.typesBien().isEmpty()) erreurs.add(prefixe + ".types-bien vide");
        if (profil.modeleDocuments() == null || profil.modeleDocuments().isBlank()) {
            erreurs.add(prefixe + ".modele-documents manquant");
        } else if (!VERSION_MODELE.matcher(profil.modeleDocuments()).matches()
                || !profil.modeleDocuments().startsWith(pays.codeMinuscule() + "-")) {
            erreurs.add(prefixe + ".modele-documents doit s'écrire « " + pays.codeMinuscule() + "-v<n> »");
        } else {
            // J5 : chaque modèle de la version courante doit exister (sinon aucun PDF ne pourrait être généré)
            for (TypeDocument type : TypeDocument.values()) {
                String chemin = "templates/" + cheminGabarit(type, pays, profil.modeleDocuments()) + ".html";
                if (!new ClassPathResource(chemin).exists()) {
                    erreurs.add(prefixe + ".modele-documents : modèle absent " + chemin);
                }
            }
        }
        if (profil.notification().canaux().isEmpty()) erreurs.add(prefixe + ".notification.canaux vide");
        champsInconnus(profil.champsObligatoires())
                .forEach(champ -> erreurs.add(prefixe + ".champs-obligatoires : champ inconnu « " + champ + " »"));
        champsInconnus(profil.champsMasques())
                .forEach(champ -> erreurs.add(prefixe + ".champs-masques : champ inconnu « " + champ + " »"));
        profil.champsObligatoires().stream()
                .filter(profil.champsMasques()::contains)
                .forEach(champ -> erreurs.add(prefixe + " : « " + champ + " » à la fois obligatoire et masqué"));
    }

    private void verifierPlafonds(Devise devise, List<String> erreurs) {
        PlafondsMontants plafond = plafonds.get(devise);
        String prefixe = "kupanga.plafonds." + devise;
        if (plafond == null) {
            erreurs.add(prefixe + " manquant (devise utilisée par un profil)");
            return;
        }
        if (!positif(plafond.loyerMensuel()) || !positif(plafond.chargesMensuelles())
                || !positif(plafond.depotGarantie())) {
            erreurs.add(prefixe + " : loyer-mensuel, charges-mensuelles et depot-garantie doivent être positifs");
        }
    }

    private static boolean positif(BigDecimal valeur) {
        return valeur != null && valeur.signum() > 0;
    }

    private static List<String> champsInconnus(Set<String> champs) {
        return champs.stream().filter(champ -> !CHAMPS_DU_BIEN.contains(champ)).sorted().toList();
    }

}
