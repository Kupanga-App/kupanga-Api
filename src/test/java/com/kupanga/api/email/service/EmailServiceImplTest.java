package com.kupanga.api.email.service;

import com.kupanga.api.email.dto.EmailSortant;
import com.kupanga.api.email.file.FileEmails;
import com.kupanga.api.email.file.PdfAJoindre;
import com.kupanga.api.email.file.TypeEmail;
import com.kupanga.api.email.service.impl.EmailServiceImpl;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static com.kupanga.api.stockage.constant.StockageConstant.QUITTANCE_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Préparation des e-mails : contenu échappé, destinataire, type et PDF joints par clé. L'envoi lui-même (file
 * persistante, nouveaux essais) est testé par {@code FileEmailsIntegrationTest} et {@code EnvoyeurEmailsTest}.
 */
@DisplayName("Tests unitaires pour EmailServiceImpl")
class EmailServiceImplTest {

    /** J3 : montants formatés comme en France (registre bouchon). */
    private static com.kupanga.api.juridiction.JuridictionRegistry registreFr() {
        com.kupanga.api.juridiction.JuridictionRegistry registre =
                org.mockito.Mockito.mock(com.kupanga.api.juridiction.JuridictionRegistry.class);
        org.mockito.Mockito.when(registre.formatMontant(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(i -> new com.kupanga.api.juridiction.FormatMontant(java.util.Locale.FRANCE,
                        i.getArgument(1) == null ? com.kupanga.api.juridiction.Devise.EUR : i.getArgument(1)));
        return registre;
    }

    private final FileEmails fileEmails = mock(FileEmails.class);

    private final EmailServiceImpl emailService = new EmailServiceImpl(
            fileEmails,
            "http://localhost:4200/auth/reset-password?token=",
            "http://localhost:4200/auth/login",
            "http://localhost:4200/"
    , registreFr());

    private EmailSortant emailMisEnFile(TypeEmail type) {
        ArgumentCaptor<EmailSortant> captor = ArgumentCaptor.forClass(EmailSortant.class);
        verify(fileEmails).ajouter(captor.capture(), eq(type), anyList());
        return captor.getValue();
    }

    @Test
    @DisplayName("sendWelcomeMessage — mis en file une fois, type BIENVENUE")
    void sendWelcomeMessage_misEnFile() {
        emailService.sendWelcomeMessage("test@example.com", "Alice");

        verify(fileEmails).ajouter(any(EmailSortant.class), eq(TypeEmail.BIENVENUE), eq(List.of()));
    }

    @Test
    @DisplayName("sendWelcomeMessage — le prénom est échappé dans le HTML (pas d'injection HTML)")
    void sendWelcomeMessage_shouldEscapeHtml() {
        emailService.sendWelcomeMessage("test@example.com", "<a href=\"http://pirate\">Alice</a>");

        assertThat(emailMisEnFile(TypeEmail.BIENVENUE).html())
                .doesNotContain("<a href=\"http://pirate\">")
                .contains("&lt;a href=&quot;http://pirate&quot;&gt;Alice&lt;/a&gt;");
    }

    @Test
    @DisplayName("sendWelcomeMessage — adressé au bon destinataire, avec une version texte brut (M1)")
    void sendWelcomeMessage_shouldSendToCorrectRecipient() {
        emailService.sendWelcomeMessage("alice@example.com", "Alice");

        EmailSortant email = emailMisEnFile(TypeEmail.BIENVENUE);
        assertThat(email.destinataire()).isEqualTo("alice@example.com");
        assertThat(email.sujet()).contains("Alice");
        assertThat(email.piecesJointes()).isEmpty();
        assertThat(email.texte()).contains("Alice").doesNotContain("<");
    }

    @Test
    @DisplayName("envoyerQuittance — PDF joint par sa clé dans le bucket privé (lu à l'envoi, jamais par URL : P0-7)")
    void envoyerQuittance_pdfParCle() {
        Quittance quittance = Quittance.builder()
                .id(3L).mois("janvier").annee(2026)
                .loyerMensuel(new BigDecimal("800.0")).chargesMensuelles(new BigDecimal("50.0")).montantTotal(new BigDecimal("850.0"))
                .bien(Bien.builder().pays(Pays.FR).adresse("1 rue A").codePostal("44000").ville("Nantes").build())
                .locataire(User.builder().firstName("Alice").lastName("Martin").mail("alice@example.com").build())
                .clePdf("cle-quittance.pdf")
                .build();

        emailService.envoyerQuittance(quittance);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PdfAJoindre>> pdfs = ArgumentCaptor.forClass(List.class);
        verify(fileEmails).ajouter(any(EmailSortant.class), eq(TypeEmail.QUITTANCE), pdfs.capture());
        assertThat(pdfs.getValue()).containsExactly(
                new PdfAJoindre(QUITTANCE_BUCKET, "cle-quittance.pdf", "Quittance_3_2026_janvier.pdf"));
    }

    @Test
    @DisplayName("sendPasswordResetMail — type RESET (lien de 10 min), lien de reset dans le HTML et le texte")
    void sendPasswordResetMail_lien() {
        emailService.sendPasswordResetMail("user@kupanga.com", "TOKEN_XYZ");

        EmailSortant email = emailMisEnFile(TypeEmail.RESET_MOT_DE_PASSE);
        assertThat(email.html()).contains("TOKEN_XYZ");
        assertThat(email.texte()).contains("http://localhost:4200/auth/reset-password?token=TOKEN_XYZ");
    }

    @Test
    @DisplayName("sendPasswordUpdatedConfirmation — mis en file, type MOT_DE_PASSE_MODIFIE")
    void sendPasswordUpdatedConfirmation_misEnFile() {
        emailService.sendPasswordUpdatedConfirmation("user@kupanga.com");

        assertThat(emailMisEnFile(TypeEmail.MOT_DE_PASSE_MODIFIE).destinataire()).isEqualTo("user@kupanga.com");
    }

    @Test
    @DisplayName("envoyerVerificationEmail — type VERIFICATION_EMAIL (critique, lien de 24 h)")
    void envoyerVerificationEmail_type() {
        emailService.envoyerVerificationEmail("user@kupanga.com", "Grâce", "JETON");

        assertThat(emailMisEnFile(TypeEmail.VERIFICATION_EMAIL).html()).contains("auth/verifier-email?token=JETON");
    }

    @Test
    @DisplayName("envoyerDemandeLocation — au locataire, type DEMANDE_LOCATION, titre échappé, lien et échéance")
    void envoyerDemandeLocation() {
        com.kupanga.api.user.entity.User proprio = com.kupanga.api.user.entity.User.builder()
                .firstName("Paul").lastName("Proprio").mail("p@test.fr").build();
        com.kupanga.api.user.entity.User loc = com.kupanga.api.user.entity.User.builder()
                .firstName("Lea").lastName("Loc").mail("l@test.fr").build();
        com.kupanga.api.immobilier.entity.Bien bien = com.kupanga.api.immobilier.entity.Bien.builder()
                .titre("<b>T2</b>").adresse("12 rue A").codePostal("44000").ville("Nantes").build();
        com.kupanga.api.immobilier.entity.DemandeLocation demande = com.kupanga.api.immobilier.entity.DemandeLocation
                .builder().id(50L).bien(bien).proprietaire(proprio).locataire(loc)
                .dateCreation(java.time.LocalDateTime.of(2026, 11, 3, 9, 5))
                .dateExpiration(java.time.LocalDateTime.of(2026, 11, 6, 9, 5)).build();

        emailService.envoyerDemandeLocation(demande);

        EmailSortant email = emailMisEnFile(TypeEmail.DEMANDE_LOCATION);
        assertThat(email.destinataire()).isEqualTo("l@test.fr");
        assertThat(email.html())
                .contains("Paul Proprio", "Lea Loc", "&lt;b&gt;T2&lt;/b&gt;", "06/11/2026 à 09h05",
                        "http://localhost:4200/demandes-location")
                .doesNotContain("<b>T2</b>");
    }
}
