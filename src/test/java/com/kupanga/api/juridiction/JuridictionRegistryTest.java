package com.kupanga.api.juridiction;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.juridiction.regle.RegleFrance;
import com.kupanga.api.juridiction.regle.RegleRdc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("J2 : JuridictionRegistry et profils de juridiction")
class JuridictionRegistryTest {

    private static ProfilJuridiction profilFr() {
        return new ProfilJuridiction(Locale.forLanguageTag("fr-FR"), ZoneId.of("Europe/Paris"),
                List.of(Devise.EUR), Devise.EUR, Set.of("codePostal"), Set.of(),
                Set.of(TypeBien.APPARTEMENT), "fr-v1",
                new ProfilJuridiction.Notification(List.of(CanalNotification.EMAIL)));
    }

    /** J3 : plafonds valides pour toutes les devises (les profils de test n'en utilisent qu'une partie). */
    private static final Map<Devise, PlafondsMontants> PLAFONDS = Map.of(
            Devise.EUR, plafonds("100000", "10000", "100000"),
            Devise.USD, plafonds("100000", "10000", "100000"),
            Devise.CDF, plafonds("300000000", "30000000", "300000000"));

    private static PlafondsMontants plafonds(String loyer, String charges, String depot) {
        return new PlafondsMontants(new BigDecimal(loyer), new BigDecimal(charges), new BigDecimal(depot));
    }

    private static JuridictionRegistry registry(Map<Pays, ProfilJuridiction> profils, RegleJuridiction... regles) {
        return new JuridictionRegistry(new JuridictionProperties(profils, PLAFONDS), List.of(regles));
    }

    /** Profils réellement chargés depuis application.yml, comme au démarrage de l'application. */
    private static JuridictionProperties proprietesDuYaml() {
        return JuridictionsDeTest.proprietesDuYaml();
    }

    @Test
    @DisplayName("application.yml : profils FR et CD chargés, cohérents, avec leurs règles")
    void yamlReel_frEtCd() throws Exception {
        JuridictionRegistry registry = new JuridictionRegistry(proprietesDuYaml(),
                List.of(new RegleFrance(), new RegleRdc()));

        assertThat(registry.paysPrisEnCharge()).containsExactly(Pays.FR, Pays.CD);

        ProfilJuridiction fr = registry.profil(Pays.FR);
        assertThat(fr.locale()).isEqualTo(Locale.forLanguageTag("fr-FR"));
        assertThat(fr.fuseau()).isEqualTo(ZoneId.of("Europe/Paris"));
        assertThat(fr.devises()).containsExactly(Devise.EUR);
        assertThat(fr.champsObligatoires()).contains("codePostal");
        assertThat(fr.modeleDocuments()).isEqualTo("fr-v1");
        assertThat(fr.notification().canaux()).containsExactly(CanalNotification.EMAIL);

        ProfilJuridiction cd = registry.profil(Pays.CD);
        assertThat(cd.fuseau()).isEqualTo(ZoneId.of("Africa/Kinshasa"));
        assertThat(cd.devises()).containsExactly(Devise.USD, Devise.CDF);
        assertThat(cd.deviseDefaut()).isEqualTo(Devise.USD);
        assertThat(cd.champsMasques()).contains("classeEnergie", "classeGes");
        assertThat(cd.modeleDocuments()).isEqualTo("cd-v1");
        assertThat(cd.notification().canaux()).containsExactly(CanalNotification.EMAIL, CanalNotification.WHATSAPP_LIEN);

        assertThat(registry.regle(Pays.CD)).isInstanceOf(RegleRdc.class);
    }

    @Test
    @DisplayName("Pays sans profil (BE, CG) → 400 « pas encore pris en charge »")
    void paysNonPrisEnCharge_400() {
        JuridictionRegistry registry = registry(Map.of(Pays.FR, profilFr()), new RegleFrance());

        assertThat(registry.estPrisEnCharge(Pays.BE)).isFalse();
        assertThatThrownBy(() -> registry.profil(Pays.BE))
                .isInstanceOf(KupangaBusinessException.class)
                .hasMessageContaining("Belgique")
                .satisfies(e -> assertThat(((KupangaBusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> registry.regle(Pays.CG)).isInstanceOf(KupangaBusinessException.class);
        assertThatThrownBy(() -> registry.profil(null)).isInstanceOf(KupangaBusinessException.class);
    }

    @Test
    @DisplayName("Démarrage refusé : profil sans règle, règle sans profil, deux règles pour un pays, aucun pays")
    void demarrageRefuse_reglesIncoherentes() {
        assertThatThrownBy(() -> registry(Map.of(Pays.FR, profilFr())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("aucune RegleJuridiction");
        assertThatThrownBy(() -> registry(Map.of(Pays.FR, profilFr()), new RegleFrance(), new RegleRdc()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("RegleJuridiction sans profil");
        assertThatThrownBy(() -> registry(Map.of(Pays.FR, profilFr()), new RegleFrance(), new RegleFrance()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Plusieurs RegleJuridiction");
        assertThatThrownBy(() -> registry(Map.of(), new RegleFrance()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("aucun pays");
    }

    @Test
    @DisplayName("Démarrage refusé : devise par défaut hors liste, champ inconnu, champ obligatoire et masqué")
    void demarrageRefuse_profilIncoherent() {
        ProfilJuridiction incoherent = new ProfilJuridiction(Locale.FRANCE, ZoneId.of("Europe/Paris"),
                List.of(Devise.EUR), Devise.USD, Set.of("codePostal", "codePostale"), Set.of("codePostal"),
                Set.of(TypeBien.MAISON), "fr-v1", new ProfilJuridiction.Notification(List.of(CanalNotification.EMAIL)));

        assertThatThrownBy(() -> registry(Map.of(Pays.FR, incoherent), new RegleFrance()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("devise-defaut")
                .hasMessageContaining("champ inconnu « codePostale »")
                .hasMessageContaining("à la fois obligatoire et masqué");
    }

    @Test
    @DisplayName("Démarrage refusé : champs vides (locale, fuseau, devises, types de bien, modèle, canaux)")
    void demarrageRefuse_champsManquants() {
        ProfilJuridiction vide = new ProfilJuridiction(null, null, null, null, null, null, null, " ", null);

        assertThatThrownBy(() -> registry(Map.of(Pays.FR, vide), new RegleFrance()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(".locale manquante")
                .hasMessageContaining(".fuseau manquant")
                .hasMessageContaining(".devises vide")
                .hasMessageContaining(".types-bien vide")
                .hasMessageContaining(".modele-documents manquant")
                .hasMessageContaining(".notification.canaux vide");
    }

    @Test
    @DisplayName("Démarrage refusé si des biens existent dans un pays sans profil")
    void demarrageRefuse_biensDansUnPaysSansProfil() throws Exception {
        JuridictionRegistry registry = registry(Map.of(Pays.FR, profilFr()), new RegleFrance());
        BienRepository bienRepository = mock(BienRepository.class);
        JuridictionConfig config = new JuridictionConfig();

        when(bienRepository.findPaysUtilises()).thenReturn(List.of(Pays.FR));
        config.verificationDesPaysEnBase(bienRepository, registry).run(null);

        when(bienRepository.findPaysUtilises()).thenReturn(List.of(Pays.FR, Pays.CG, Pays.BE));
        assertThatThrownBy(() -> config.verificationDesPaysEnBase(bienRepository, registry).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[BE, CG]");
    }
    // ══════════════════════════════════════════════════════════════
    // J3 : devises, plafonds (C5), format des montants
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("J3 : application.yml — plafonds EUR, USD et CDF chargés")
    void yamlReel_plafonds() {
        JuridictionProperties proprietes = proprietesDuYaml();

        assertThat(proprietes.plafonds().get(Devise.EUR).loyerMensuel()).isEqualByComparingTo("100000");
        assertThat(proprietes.plafonds().get(Devise.USD).chargesMensuelles()).isEqualByComparingTo("10000");
        assertThat(proprietes.plafonds().get(Devise.CDF).loyerMensuel()).isEqualByComparingTo("300000000");
        assertThat(proprietes.plafonds().get(Devise.CDF).depotGarantie()).isEqualByComparingTo("300000000");
    }

    @Test
    @DisplayName("J3 : devisePour — défaut du profil, devise du pays acceptée, devise d'un autre pays → 400")
    void devisePour() {
        JuridictionRegistry registry = JuridictionsDeTest.registre();

        assertThat(registry.devisePour(Pays.FR, null)).isEqualTo(Devise.EUR);
        assertThat(registry.devisePour(Pays.CD, null)).isEqualTo(Devise.USD);
        assertThat(registry.devisePour(Pays.CD, Devise.CDF)).isEqualTo(Devise.CDF);
        assertThatThrownBy(() -> registry.devisePour(Pays.FR, Devise.USD))
                .isInstanceOf(KupangaBusinessException.class)
                .hasMessageContaining("USD")
                .satisfies(e -> assertThat(((KupangaBusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> registry.devisePour(Pays.CD, Devise.EUR)).isInstanceOf(KupangaBusinessException.class);
        assertThatThrownBy(() -> registry.devisePour(Pays.BE, null)).isInstanceOf(KupangaBusinessException.class);
    }

    @Test
    @DisplayName("C5 : plafonds par devise — 150 000 refusé en USD, accepté en CDF ; limite incluse ; null ignoré")
    void verifierMontants_parDevise() {
        JuridictionRegistry registry = JuridictionsDeTest.registre();
        BigDecimal loyer = new BigDecimal("150000");

        assertThatThrownBy(() -> registry.verifierMontants(Devise.USD, loyer, null, null))
                .isInstanceOf(KupangaBusinessException.class)
                .hasMessageContaining("loyer mensuel")
                .hasMessageContaining("100000 USD")
                .satisfies(e -> assertThat(((KupangaBusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        registry.verifierMontants(Devise.CDF, loyer, null, null);

        registry.verifierMontants(Devise.EUR, new BigDecimal("100000.00"), new BigDecimal("10000"), new BigDecimal("100000"));
        assertThatThrownBy(() -> registry.verifierMontants(Devise.EUR, null, new BigDecimal("10000.01"), null))
                .isInstanceOf(KupangaBusinessException.class).hasMessageContaining("charges");
        assertThatThrownBy(() -> registry.verifierMontants(Devise.EUR, null, null, new BigDecimal("100000.01")))
                .isInstanceOf(KupangaBusinessException.class).hasMessageContaining("dépôt de garantie");
        registry.verifierMontants(Devise.EUR, null, null, null);
        assertThatThrownBy(() -> registry.verifierMontants(Devise.XAF, BigDecimal.ONE, null, null))
                .isInstanceOf(KupangaBusinessException.class).hasMessageContaining("XAF");
    }

    @Test
    @DisplayName("J3 : démarrage refusé si une devise d'un profil n'a pas de plafonds, ou des plafonds non positifs")
    void demarrageRefuse_plafonds() {
        assertThatThrownBy(() -> new JuridictionRegistry(new JuridictionProperties(Map.of(Pays.FR, profilFr())),
                List.of(new RegleFrance())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kupanga.plafonds.EUR manquant");

        Map<Devise, PlafondsMontants> negatifs = Map.of(Devise.EUR, plafonds("0", "10", "-1"));
        assertThatThrownBy(() -> new JuridictionRegistry(new JuridictionProperties(Map.of(Pays.FR, profilFr()), negatifs),
                List.of(new RegleFrance())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kupanga.plafonds.EUR : loyer-mensuel");
    }

    @Test
    @DisplayName("J3 : formatMontant — langue du pays, devise figée du document")
    void formatMontant() {
        JuridictionRegistry registry = JuridictionsDeTest.registre();

        assertThat(registry.formatMontant(Pays.FR, Devise.EUR).f(new BigDecimal("1234.5"))).isEqualTo("1 234,50 €");
        assertThat(registry.formatMontant(Pays.FR, Devise.EUR).somme(new BigDecimal("850"), null)).isEqualTo("850,00 €");
        assertThat(registry.formatMontant(Pays.FR, Devise.EUR).f(null)).isEmpty();
        assertThat(registry.formatMontant(Pays.CD, Devise.USD).f(new BigDecimal("1200")))
                .contains("1 200,00").contains("$").doesNotContain("€");
        assertThat(registry.formatMontant(Pays.CD, Devise.CDF).f(new BigDecimal("250000")))
                .contains("250 000,00").doesNotContain("€").doesNotContain("$");
    }

    // ══════════════════════════════════════════════════════════════
    // J4 : contrôle d'un formulaire selon le pays
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("J4 : controlerBien à la modification — obligatoires non exigés (null = inchangé), masqués refusés")
    void controlerBien_modification() {
        JuridictionRegistry registry = JuridictionsDeTest.registre();
        com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO miseAJour =
                new com.kupanga.api.immobilier.dto.formDTO.BienUpdateDTO();

        assertThat(registry.controlerBien(Pays.CD, miseAJour, false)).isEmpty();

        miseAJour.setClasseGes(com.kupanga.api.immobilier.entity.ClasseGes.D);
        assertThat(registry.controlerBien(Pays.CD, miseAJour, false))
                .extracting(ViolationChamp::champ).containsExactly("classeGes");
        assertThat(registry.controlerBien(Pays.FR, miseAJour, false)).isEmpty();

        // Plafonds non contrôlés ici à la modification (le service les revérifie avec les montants existants)
        miseAJour.setClasseGes(null);
        miseAJour.setLoyerMensuel(new BigDecimal("1000000"));
        assertThat(registry.controlerBien(Pays.FR, miseAJour, false)).isEmpty();

        assertThatThrownBy(() -> registry.verifierBien(Pays.CD,
                com.kupanga.api.immobilier.dto.formDTO.BienFormDTO.builder().build(), true))
                .isInstanceOf(KupangaBusinessException.class)
                .hasMessageContaining("commune : Ce champ est obligatoire");
        assertThat(registry.controlerBien(Pays.BE, miseAJour, true)).isEmpty();
    }

    @Test
    @DisplayName("J4 : application.yml — champs obligatoires et masqués de la France et de la RDC")
    void yamlReel_champsAdresse() {
        JuridictionRegistry registry = JuridictionsDeTest.registre();

        assertThat(registry.profil(Pays.FR).champsObligatoires()).containsExactly("codePostal");
        assertThat(registry.profil(Pays.FR).champsMasques())
                .containsExactlyInAnyOrder("commune", "quartier", "avenue", "numeroParcelle", "pointDeRepere");
        assertThat(registry.profil(Pays.CD).champsObligatoires()).containsExactlyInAnyOrder("commune", "quartier");
        assertThat(registry.profil(Pays.CD).champsMasques())
                .containsExactlyInAnyOrder("codePostal", "classeEnergie", "classeGes");
    }

    @Test
    @DisplayName("J5 : démarrage refusé si la version des modèles est mal nommée ou si un modèle manque")
    void demarrageRefuse_modelesDocuments() {
        for (String version : new String[]{"cd-v1", "fr-1", "fr-v9"}) {
            ProfilJuridiction profil = new ProfilJuridiction(Locale.forLanguageTag("fr-FR"), ZoneId.of("Europe/Paris"),
                    List.of(Devise.EUR), Devise.EUR, Set.of("codePostal"), Set.of(), Set.of(TypeBien.APPARTEMENT),
                    version, new ProfilJuridiction.Notification(List.of(CanalNotification.EMAIL)));

            assertThatThrownBy(() -> registry(Map.of(Pays.FR, profil), new RegleFrance()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(".modele-documents")
                    .hasMessageContaining(version.equals("fr-v9")
                            ? "modèle absent templates/documents/fr/fr-v9/contrat.html"
                            : "doit s'écrire « fr-v<n> »");
        }
    }
}
