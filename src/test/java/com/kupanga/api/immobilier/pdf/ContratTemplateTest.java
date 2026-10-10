package com.kupanga.api.immobilier.pdf;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.entity.StatutQuittance;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.immobilier.entity.TypeEtat;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.TypeDocument;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;

/**
 * Rendu réel des modèles de documents par pays ({@code templates/documents/<pays>/<version>/}), choisis par le
 * registre comme dans {@link ContratPdfService}, {@link QuittancePdfService} et {@link EtatDesLieuxPdfService}
 * (mêmes variables, sans contexte Spring).
 */
@DisplayName("Modèles de documents par pays (bail, quittance, état des lieux)")
class ContratTemplateTest {

    private final JuridictionRegistry registre = JuridictionsDeTest.registre();

    private final User proprio = User.builder().id(1L).firstName("Paul").lastName("Proprio").mail("p@test.fr")
            .role(Role.ROLE_PROPRIETAIRE).build();
    private final User locataire = User.builder().id(2L).firstName("Lea").lastName("Loc").mail("l@test.fr")
            .role(Role.ROLE_LOCATAIRE).build();

    private static String rendre(String gabarit, Context ctx) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine moteur = new SpringTemplateEngine();
        moteur.setTemplateResolver(resolver);
        return moteur.process(gabarit, ctx);
    }

    private String rendre(Contrat contrat) {
        Context ctx = new Context();
        ctx.setVariable("contrat", contrat);
        ctx.setVariable("montants", registre.formatMontant(contrat.getPays(), contrat.getDevise()));
        ctx.setVariable("proprietaire", contrat.getProprietaire());
        ctx.setVariable("locataire", contrat.getLocataire());
        return rendre(registre.gabarit(TypeDocument.CONTRAT, contrat.getPays(), contrat.getModeleVersion()), ctx);
    }

    private String rendre(Quittance quittance) {
        Context ctx = new Context();
        ctx.setVariable("quittance", quittance);
        ctx.setVariable("montants", registre.formatMontant(quittance.getPays(), quittance.getDevise()));
        ctx.setVariable("proprietaire", quittance.getProprietaire());
        ctx.setVariable("locataire", quittance.getLocataire());
        ctx.setVariable("moisLabel", quittance.getMois());
        return rendre(registre.gabarit(TypeDocument.QUITTANCE, quittance.getPays(), quittance.getModeleVersion()), ctx);
    }

    private String rendre(EtatDesLieux edl) {
        Context ctx = new Context();
        ctx.setVariable("edl", edl);
        ctx.setVariable("proprietaire", edl.getProprietaire());
        ctx.setVariable("locataire", edl.getLocataire());
        return rendre(registre.gabarit(TypeDocument.ETAT_DES_LIEUX, edl.getPays(), edl.getModeleVersion()), ctx);
    }

    private EtatDesLieux edl(Bien bien) {
        return EtatDesLieux.builder().id(6L).bien(bien).proprietaire(proprio).locataire(locataire)
                .pays(bien.getPays()).modeleVersion(bien.getPays().codeMinuscule() + "-v1")
                .type(TypeEtat.ENTREE).dateRealisation(LocalDate.of(2026, 1, 2))
                .pieces(new HashSet<>()).compteurs(new HashSet<>()).cles(new HashSet<>())
                .build();
    }

    private Bien bien(Pays pays, Devise devise) {
        return Bien.builder().id(3L).titre("T2").typeBien(TypeBien.APPARTEMENT)
                .adresse("12 rue des Tests").codePostal("44000").ville("Nantes").pays(pays).devise(devise)
                .ascenseur(false).meuble(false).colocation(false)
                .proprietaire(proprio).build();
    }

    private Contrat contrat(Bien bien) {
        return Contrat.builder().id(4L).bien(bien).proprietaire(proprio).locataire(locataire)
                .pays(bien.getPays()).devise(bien.getDevise()).modeleVersion(bien.getPays().codeMinuscule() + "-v1")
                .dateDebut(LocalDate.of(2026, 1, 1)).dureeBailMois(12)
                .loyerMensuel(new BigDecimal("1200.00")).chargesMensuelles(new BigDecimal("50.50"))
                .depotGarantie(new BigDecimal("800"))
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_PROPRIO).build();
    }

    @Test
    @DisplayName("J1 : le pays du bien figure en toutes lettres (« France »), jamais en code ISO")
    void paysEnToutesLettres() {
        String html = rendre(contrat(bien(Pays.FR, Devise.EUR)));

        assertThat(html).contains("12 rue des Tests, 44000 Nantes, France");
        assertThat(html).doesNotContain("Nantes, FR<");
    }

    @Test
    @DisplayName("J4 : bien à Kinshasa — adresse congolaise sur le bail et la quittance, jamais « null » sans code postal")
    void adresseCongolaise_sansCodePostal() {
        Bien bien = bien(Pays.CD, Devise.USD);
        bien.setAdresse("N° 12, Av. Kasa-Vubu");
        bien.setVille("Kinshasa");
        bien.setCodePostal(null);
        bien.setCommune("Kalamu");
        bien.setQuartier("Matonge");
        Quittance quittance = Quittance.builder().id(5L).bien(bien).proprietaire(proprio).locataire(locataire)
                .pays(Pays.CD).devise(Devise.USD).modeleVersion("cd-v1").mois("janvier").annee(2026)
                .loyerMensuel(new BigDecimal("500")).chargesMensuelles(BigDecimal.ZERO)
                .montantTotal(new BigDecimal("500")).dateEcheance(LocalDate.of(2026, 1, 5))
                .statut(StatutQuittance.EN_ATTENTE).build();

        String bail = rendre(contrat(bien));
        String recu = rendre(quittance);

        String adresse = "N° 12, Av. Kasa-Vubu, quartier Matonge, commune de Kalamu, Kinshasa";
        assertThat(bail).contains(adresse + ", République démocratique du Congo");
        assertThat(recu).contains(adresse);
        // Ni code postal « null » devant la ville, ni champ vide dans l'adresse
        assertThat(bail).doesNotContain("null Kinshasa", "null —", ", null");
        assertThat(recu).doesNotContain("null Kinshasa", ", null");
    }

    @Test
    @DisplayName("J3 : bail en euros — montants exacts formatés à la française (centimes, total loyer + charges)")
    void contrat_montantsEnEuros() {
        String html = rendre(contrat(bien(Pays.FR, Devise.EUR)));

        assertThat(html).contains("1 200,00 €", "50,50 €", "1 250,50 €", "800,00 €");
    }

    @Test
    @DisplayName("J3 : bail en dollars (RDC) — devise figée du contrat, aucun « € »")
    void contrat_montantsEnDollars() {
        String html = rendre(contrat(bien(Pays.CD, Devise.USD)));

        assertThat(html).contains("1 200,00").contains("1 250,50").contains("$");
        assertThat(html).doesNotContain("€");
    }

    @Test
    @DisplayName("J3 : quittance en francs congolais — devise figée de la quittance, aucun « € »")
    void quittance_montantsEnFrancsCongolais() {
        Quittance quittance = Quittance.builder().id(5L).bien(bien(Pays.CD, Devise.CDF))
                .proprietaire(proprio).locataire(locataire)
                .pays(Pays.CD).devise(Devise.CDF).modeleVersion("cd-v1")
                .mois("janvier").annee(2026)
                .loyerMensuel(new BigDecimal("250000")).chargesMensuelles(new BigDecimal("10000"))
                .montantTotal(new BigDecimal("260000"))
                .dateEcheance(LocalDate.of(2026, 1, 5))
                .statut(StatutQuittance.EN_ATTENTE).build();

        String html = rendre(quittance);

        assertThat(html).contains("250 000,00").contains("260 000,00");
        assertThat(html).doesNotContain("€");
    }

    // ══════════════════════════════════════════════════════════════
    // J5 : un modèle par pays, version figée sur le document
    // ══════════════════════════════════════════════════════════════

    /** Bien à Kinshasa : adresse congolaise, ni code postal, ni étage, ni DPE. */
    private Bien bienKinshasa() {
        Bien bien = bien(Pays.CD, Devise.USD);
        bien.setAdresse("N° 12, Av. Kasa-Vubu");
        bien.setVille("Kinshasa");
        bien.setCodePostal(null);
        bien.setCommune("Kalamu");
        bien.setQuartier("Matonge");
        bien.setPointDeRepere("En face de la station");
        bien.setAscenseur(null);
        bien.setMeuble(null);
        bien.setColocation(null);
        return bien;
    }

    @Test
    @DisplayName("C9 : bail RDC = modèle provisoire, bandeau « À FAIRE VALIDER PAR UN JURISTE », aucune référence au droit français")
    void bailRdc_provisoire_sansDroitFrancais() {
        String html = rendre(contrat(bienKinshasa()));

        assertThat(html).contains("MODÈLE PROVISOIRE — À FAIRE VALIDER PAR UN JURISTE");
        assertThat(html).contains("À RÉDIGER ET À FAIRE VALIDER PAR UN JURISTE");
        assertThat(html).contains("Obligations du bailleur", "Durée du préavis et conditions de résiliation");
        assertThat(html).doesNotContain("89-462", "6 juillet 1989", "Diagnostic de performance énergétique");
        // Adresse congolaise, champs facultatifs absents sans « null »
        assertThat(html).contains("Matonge", "Kalamu", "En face de la station");
        assertThat(html).doesNotContain(">null<", "nullᵉ");
    }

    @Test
    @DisplayName("J5 : bail FR = modèle existant, sans bandeau provisoire ; valeurs facultatives absentes sans « null »")
    void bailFrance_modeleExistant() {
        Bien bien = bien(Pays.FR, Devise.EUR);
        bien.setAscenseur(null);
        bien.setMeuble(null);

        String html = rendre(contrat(bien));

        assertThat(html).contains("loi n° 89-462 du 6 juillet 1989", "Diagnostic de performance énergétique");
        assertThat(html).doesNotContain("MODÈLE PROVISOIRE", "À RÉDIGER");
        assertThat(html).doesNotContain("nullᵉ", "construit en <strong></strong>");
    }

    @Test
    @DisplayName("C9 : quittance et état des lieux RDC provisoires, sans référence au droit français ; EDL FR inchangé")
    void quittanceEtEdlRdc_provisoires() {
        Quittance quittance = Quittance.builder().id(5L).bien(bienKinshasa()).proprietaire(proprio).locataire(locataire)
                .pays(Pays.CD).devise(Devise.USD).modeleVersion("cd-v1").mois("janvier").annee(2026)
                .loyerMensuel(new BigDecimal("500")).chargesMensuelles(BigDecimal.ZERO)
                .montantTotal(new BigDecimal("500")).dateEcheance(LocalDate.of(2026, 1, 5))
                .statut(StatutQuittance.EN_ATTENTE).build();

        String recu = rendre(quittance);
        String edlRdc = rendre(edl(bienKinshasa()));
        String edlFrance = rendre(edl(bien(Pays.FR, Devise.EUR)));

        assertThat(recu).contains("À FAIRE VALIDER PAR UN JURISTE").doesNotContain("article 21", "1989");
        assertThat(edlRdc).contains("À FAIRE VALIDER PAR UN JURISTE").doesNotContain("89-462");
        assertThat(edlFrance).contains("89-462").doesNotContain("MODÈLE PROVISOIRE");
    }

    @Test
    @DisplayName("J5 : les modèles FR et CD (fragments communs compris) se convertissent en PDF (XHTML valide pour Flying Saucer)")
    void conversionPdf_frEtCd() throws Exception {
        for (String html : new String[]{rendre(contrat(bien(Pays.FR, Devise.EUR))), rendre(contrat(bienKinshasa())),
                rendre(edl(bienKinshasa()))}) {
            try (java.io.ByteArrayOutputStream pdf = new java.io.ByteArrayOutputStream()) {
                org.xhtmlrenderer.pdf.ITextRenderer moteur = new org.xhtmlrenderer.pdf.ITextRenderer();
                moteur.setDocumentFromString(html);
                moteur.layout();
                moteur.createPDF(pdf);
                assertThat(new String(pdf.toByteArray(), 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                        .isEqualTo("%PDF-");
            }
        }
    }

    @Test
    @DisplayName("J5 : le modèle suit la version figée sur le document ; version invalide ou d'un autre pays refusée")
    void gabarit_versionFigee() {
        assertThat(registre.gabarit(TypeDocument.CONTRAT, Pays.CD, "cd-v1")).isEqualTo("documents/cd/cd-v1/contrat");
        assertThat(registre.gabarit(TypeDocument.ETAT_DES_LIEUX, Pays.FR, null))
                .isEqualTo("documents/fr/fr-v1/etat-des-lieux");

        for (String version : new String[]{"fr-v1", "../../backoffice/login", "cd-v1/../../x", "CD-V1", "cd-v"}) {
            assertThatThrownBy(() -> registre.gabarit(TypeDocument.CONTRAT, Pays.CD, version))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
