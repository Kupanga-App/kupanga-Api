package com.kupanga.api.notification.whatsapp;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("C7-C16 : LienWhatsAppServiceImpl")
class LienWhatsAppServiceImplTest {

    private static final String URL_CONNEXION = "https://kupanga.test/auth/login";
    private static final String TOKEN = "jeton-secret-de-signature";

    @Mock private ContratRepository contratRepository;
    @Mock private EtatDesLieuxRepository edlRepository;
    @Mock private QuittanceRepository quittanceRepository;
    @Mock private BienService bienService;

    private LienWhatsAppServiceImpl service;
    private User proprietaire;
    private User locataire;
    private Bien bienCd;

    @BeforeEach
    void setUp() {
        service = new LienWhatsAppServiceImpl(contratRepository, edlRepository, quittanceRepository, bienService,
                JuridictionsDeTest.registre(), URL_CONNEXION);
        proprietaire = User.builder().id(1L).mail("proprio@test.cd").firstName("Jean").build();
        locataire = User.builder().id(2L).mail("locataire@test.cd").firstName("Grâce").telephone("+243812345678").build();
        bienCd = Bien.builder().id(10L).pays(Pays.CD).adresse("12").avenue("Kasa-Vubu")
                .quartier("Matonge").commune("Kalamu").ville("Kinshasa").proprietaire(proprietaire).build();
    }

    private Contrat contrat(Pays pays, StatutContrat statut, LocalDateTime expiration) {
        return Contrat.builder().id(5L).pays(pays).devise(Devise.USD).bien(bienCd)
                .proprietaire(proprietaire).locataire(locataire).adresseBien("12, avenue Kasa-Vubu, Kinshasa")
                .statut(statut).tokenSignature(TOKEN).tokenExpiration(expiration).build();
    }

    @Test
    @DisplayName("bail en attente du locataire (RDC) : lien vers son numéro, sans le jeton de signature")
    void invitationContrat_sansJeton() {
        when(contratRepository.findById(5L)).thenReturn(Optional.of(
                contrat(Pays.CD, StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE, LocalDateTime.now().plusHours(10))));

        LienWhatsAppDTO lien = service.invitationContrat(5L, "proprio@test.cd");

        assertThat(lien.numeroConnu()).isTrue();
        assertThat(lien.url()).startsWith("https://wa.me/243812345678?text=");
        assertThat(lien.texte()).contains("Grâce", "12, avenue Kasa-Vubu, Kinshasa", URL_CONNEXION)
                .doesNotContain(TOKEN).doesNotContain("signer/");
        assertThat(lien.url()).doesNotContain(TOKEN);
        verify(bienService).verifierDocumentModifiable(bienCd, proprietaire, locataire);
    }

    @Test
    @DisplayName("locataire sans numéro : lien wa.me sans destinataire")
    void invitationContrat_sansNumero() {
        locataire.setTelephone(null);
        when(contratRepository.findById(5L)).thenReturn(Optional.of(
                contrat(Pays.CD, StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE, LocalDateTime.now().plusHours(10))));

        LienWhatsAppDTO lien = service.invitationContrat(5L, "proprio@test.cd");

        assertThat(lien.numeroConnu()).isFalse();
        assertThat(lien.url()).startsWith("https://wa.me/?text=");
    }

    @Test
    @DisplayName("contrat d'un autre propriétaire : 403, avant tout autre contrôle")
    void invitationContrat_autreProprietaire_403() {
        when(contratRepository.findById(5L)).thenReturn(Optional.of(
                contrat(Pays.CD, StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE, LocalDateTime.now().plusHours(10))));

        assertThatThrownBy(() -> service.invitationContrat(5L, "intrus@test.cd"))
                .isInstanceOf(KupangaBusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(bienService);
    }

    @Test
    @DisplayName("contrat inconnu : 404")
    void invitationContrat_inconnu_404() {
        when(contratRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.invitationContrat(5L, "proprio@test.cd"))
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("bail français : 409, pas de WhatsApp en France")
    void invitationContrat_france_409() {
        when(contratRepository.findById(5L)).thenReturn(Optional.of(
                contrat(Pays.FR, StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE, LocalDateTime.now().plusHours(10))));

        assertThatThrownBy(() -> service.invitationContrat(5L, "proprio@test.cd"))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("bail signé, en attente du propriétaire ou lien expiré : 409")
    void invitationContrat_mauvaisStatut_409() {
        for (Contrat c : new Contrat[]{
                contrat(Pays.CD, StatutContrat.SIGNE, null),
                contrat(Pays.CD, StatutContrat.EN_ATTENTE_SIGNATURE_PROPRIO, null),
                contrat(Pays.CD, StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE, LocalDateTime.now().minusMinutes(1))}) {
            when(contratRepository.findById(5L)).thenReturn(Optional.of(c));
            assertThatThrownBy(() -> service.invitationContrat(5L, "proprio@test.cd"))
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Test
    @DisplayName("bien archivé ou partie supprimée : refus de verifierDocumentModifiable propagé (409)")
    void invitationContrat_bienArchive_409() {
        when(contratRepository.findById(5L)).thenReturn(Optional.of(
                contrat(Pays.CD, StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE, LocalDateTime.now().plusHours(10))));
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierDocumentModifiable(any(), any(), any());

        assertThatThrownBy(() -> service.invitationContrat(5L, "proprio@test.cd"))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("EDL de sortie en attente du locataire : lien sans jeton, adresse congolaise complète")
    void invitationEdl() {
        EtatDesLieux edl = EtatDesLieux.builder().id(7L).pays(Pays.CD).bien(bienCd).type(TypeEtat.SORTIE)
                .proprietaire(proprietaire).locataire(locataire).statut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE)
                .tokenSignature(TOKEN).tokenExpiration(LocalDateTime.now().plusHours(10)).build();
        when(edlRepository.findById(7L)).thenReturn(Optional.of(edl));

        LienWhatsAppDTO lien = service.invitationEtatDesLieux(7L, "proprio@test.cd");

        assertThat(lien.texte()).contains("état des lieux de sortie", "quartier Matonge", "commune de Kalamu")
                .doesNotContain(TOKEN);
        assertThat(lien.url()).startsWith("https://wa.me/243812345678?text=");
    }

    @Test
    @DisplayName("EDL d'un autre propriétaire : 403")
    void invitationEdl_autreProprietaire_403() {
        EtatDesLieux edl = EtatDesLieux.builder().id(7L).pays(Pays.CD).bien(bienCd)
                .proprietaire(proprietaire).locataire(locataire).statut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE).build();
        when(edlRepository.findById(7L)).thenReturn(Optional.of(edl));

        assertThatThrownBy(() -> service.invitationEtatDesLieux(7L, "intrus@test.cd"))
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Quittance quittance(StatutQuittance statut) {
        return Quittance.builder().id(9L).pays(Pays.CD).devise(Devise.USD).bien(bienCd)
                .proprietaire(proprietaire).locataire(locataire).mois("mars").annee(2026)
                .montantTotal(new BigDecimal("450.00")).statut(statut).build();
    }

    @Test
    @DisplayName("rappel de loyer non payé : mois, montant formaté dans la devise de la quittance")
    void rappelLoyer() {
        when(quittanceRepository.findById(9L)).thenReturn(Optional.of(quittance(StatutQuittance.EN_RETARD)));

        LienWhatsAppDTO lien = service.rappelLoyer(9L, "proprio@test.cd");

        assertThat(lien.texte()).contains("Grâce", "mars 2026", "450,00").contains("US");
        assertThat(lien.url()).startsWith("https://wa.me/243812345678?text=");
    }

    @Test
    @DisplayName("rappel d'un loyer déjà payé : 409")
    void rappelLoyer_payee_409() {
        when(quittanceRepository.findById(9L)).thenReturn(Optional.of(quittance(StatutQuittance.PAYEE)));

        assertThatThrownBy(() -> service.rappelLoyer(9L, "proprio@test.cd"))
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("rappel sur la quittance d'un autre propriétaire : 403")
    void rappelLoyer_autreProprietaire_403() {
        when(quittanceRepository.findById(9L)).thenReturn(Optional.of(quittance(StatutQuittance.EN_ATTENTE)));

        assertThatThrownBy(() -> service.rappelLoyer(9L, "intrus@test.cd"))
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }
}
