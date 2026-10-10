package com.kupanga.api.immobilier.service;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.ContratFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.immobilier.mapper.ContratMapper;
import com.kupanga.api.immobilier.pdf.ContratPdfService;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.service.impl.ContratServiceImpl;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;

@DisplayName("Tests unitaires — ContratServiceImpl")
class ContratServiceImplTest {

    @Mock private ContratRepository   contratRepository;
    @Mock private ContratPdfService   contratPdfService;
    @Mock private EmailService        emailService;
    @Mock private UserService         userService;
    @Mock private BienService         bienService;
    @Mock private ContratMapper       contratMapper;
    @Mock private NotificationService notificationService;
    /** J3 : vrai registre (profils et plafonds de application.yml). */
    @Spy  private JuridictionRegistry juridictionRegistry = JuridictionsDeTest.registre();

    @InjectMocks
    private ContratServiceImpl contratService;

    private User proprietaire;
    private User locataire;
    private Bien bien;
    private Contrat contrat;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        proprietaire = User.builder()
                .id(1L)
                .mail("proprio@test.com")
                .build();

        locataire = User.builder()
                .id(2L)
                .mail("locataire@test.com")
                .build();

        bien = Bien.builder().pays(Pays.FR).devise(Devise.EUR)
                .id(1L)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .build();

        contrat = Contrat.builder()
                .id(1L)
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .loyerMensuel(new BigDecimal("850.0"))
                .chargesMensuelles(new BigDecimal("50.0"))
                .depotGarantie(new BigDecimal("1700.0"))
                .statut(StatutContrat.EN_ATTENTE_SIGNATURE_PROPRIO)
                .tokenSignature("token-valide")
                .tokenExpiration(LocalDateTime.now().plusHours(48))
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // creerContrat
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("creerContrat() — succès : contrat créé et PDF généré")
    void creerContrat_success_savesContratWithPdf() {
        ContratFormDTO dto = buildValidContratFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(bien, locataire.getMail())).thenReturn(locataire);
        when(contratPdfService.genererEtUploaderPdf(any(Contrat.class))).thenReturn("http://minio/contrat.pdf");
        when(contratRepository.save(any(Contrat.class))).thenReturn(contrat);

        assertDoesNotThrow(() -> contratService.creerContrat(dto, proprietaire.getMail()));

        verify(contratPdfService).genererEtUploaderPdf(any(Contrat.class));
        verify(contratRepository).save(any(Contrat.class));
    }

    @Test
    @DisplayName("J3 : creerContrat() — pays, devise et version du modèle du bien figés sur le bail")
    void creerContrat_figeJuridiction() {
        bien.setPays(Pays.CD);
        bien.setDevise(Devise.CDF);
        ContratFormDTO dto = buildValidContratFormDTO();
        dto.setLoyerMensuel(new BigDecimal("250000"));

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(bien, locataire.getMail())).thenReturn(locataire);
        when(contratPdfService.genererEtUploaderPdf(any(Contrat.class))).thenReturn("contrat.pdf");

        contratService.creerContrat(dto, proprietaire.getMail());

        ArgumentCaptor<Contrat> captor = ArgumentCaptor.forClass(Contrat.class);
        verify(contratRepository).save(captor.capture());
        assertThat(captor.getValue().getPays()).isEqualTo(Pays.CD);
        assertThat(captor.getValue().getDevise()).isEqualTo(Devise.CDF);
        assertThat(captor.getValue().getModeleVersion()).isEqualTo("cd-v1");
        assertThat(captor.getValue().getLoyerMensuel()).isEqualByComparingTo("250000");
    }

    @Test
    @DisplayName("C5 : creerContrat() — loyer au-dessus du plafond de la devise du bien → 400, ni PDF ni contrat")
    void creerContrat_plafondDepasse_400() {
        ContratFormDTO dto = buildValidContratFormDTO();
        dto.setLoyerMensuel(new BigDecimal("100000.01"));

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(bien, locataire.getMail())).thenReturn(locataire);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.creerContrat(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(contratPdfService);
        verify(contratRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerContrat() — bien introuvable → KupangaBusinessException 404")
    void creerContrat_bienNotFound_throwsException() {
        ContratFormDTO dto = buildValidContratFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail()))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.creerContrat(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(contratRepository, never()).save(any());
        verify(contratRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("creerContrat() — bien d'un autre propriétaire → 403, aucun contrat créé (P0-5)")
    void creerContrat_bienDAutrui_throwsForbidden() {
        ContratFormDTO dto = buildValidContratFormDTO();

        when(userService.getUserByEmail("autre@test.com")).thenReturn(User.builder().id(9L).mail("autre@test.com").build());
        when(bienService.verifierProprietaire(1L, "autre@test.com"))
                .thenThrow(new KupangaBusinessException("Accès refusé", HttpStatus.FORBIDDEN));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.creerContrat(dto, "autre@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(contratPdfService, never()).genererEtUploaderPdf(any());
        verify(contratRepository, never()).save(any());
        verify(contratRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("creerContrat() — e-mail locataire différent du locataire du bien → 400 (P0-5)")
    void creerContrat_mauvaisLocataire_throwsBadRequest() {
        ContratFormDTO dto = buildValidContratFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(bien, locataire.getMail()))
                .thenThrow(new KupangaBusinessException("Mauvais locataire", HttpStatus.BAD_REQUEST));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.creerContrat(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(contratRepository, never()).save(any());
        verify(contratRepository, never()).saveAndFlush(any());
    }

    // ══════════════════════════════════════════════════════════════
    // getContratParToken
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getContratParToken() — token valide, statut correct → ContratDTO retourné")
    void getContratParToken_valid_returnsDTO() {
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        ContratDTO dto = mock(ContratDTO.class);
        when(dto.id()).thenReturn(1L);

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        when(contratMapper.toDTO(contrat)).thenReturn(dto);

        ContratDTO result = contratService.getContratParToken("token-valide");

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getContratParToken() — token introuvable → KupangaBusinessException 404 (B7)")
    void getContratParToken_tokenNotFound_throwsException() {
        when(contratRepository.findByTokenSignature("mauvais-token")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.getContratParToken("mauvais-token"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getContratParToken() — token expiré → passage à EXPIRE + KupangaBusinessException 410 (B7)")
    void getContratParToken_expired_setsExpiredAndThrows() {
        contrat.setTokenExpiration(LocalDateTime.now().minusMinutes(1));
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        when(contratRepository.save(contrat)).thenReturn(contrat);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.getContratParToken("token-valide"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.GONE);
        // EXPIRE enregistré dans sa propre transaction (sinon annulé avec le refus)
        verify(contratRepository).marquerExpire(contrat.getId(), contrat.getVersion());
        verify(contratRepository, never()).save(any());
    }

    @Test
    @DisplayName("getContratParToken() — statut != EN_ATTENTE_SIGNATURE_LOCATAIRE → 409 métier")
    void getContratParToken_wrongStatut_throwsConflict() {
        contrat.setStatut(StatutContrat.SIGNE);

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.getContratParToken("token-valide"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ══════════════════════════════════════════════════════════════
    // signerProprietaire
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("signerProprietaire() — succès : signature enregistrée, PDF régénéré, mail envoyé")
    void signerProprietaire_success() {
        when(contratRepository.findById(1L)).thenReturn(Optional.of(contrat));
        when(contratPdfService.genererEtUploaderPdf(contrat)).thenReturn("http://minio/signed.pdf");
        when(contratRepository.save(contrat)).thenReturn(contrat);
        doNothing().when(emailService).envoyerInvitationSignature(any(Contrat.class), anyString());

        assertDoesNotThrow(() ->
                contratService.signerProprietaire(1L, "sig-base64", proprietaire.getMail()));

        assertThat(contrat.getSignatureProprietaire()).isEqualTo("sig-base64");
        assertThat(contrat.getStatut()).isEqualTo(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        assertThat(contrat.getTokenSignature()).isNotNull();
        assertThat(contrat.getClePdf()).isEqualTo("http://minio/signed.pdf");
        verify(emailService).envoyerInvitationSignature(any(Contrat.class), anyString());
        verify(notificationService).saveAndSend(
                eq(locataire), eq(NotificationType.INVITATION_SIGNATURE_CONTRAT),
                anyString(), anyString(), anyString(), eq(contrat.getId()));
    }

    @Test
    @DisplayName("signerProprietaire() — contrat introuvable → KupangaBusinessException 404")
    void signerProprietaire_contratNotFound_throwsException() {
        when(contratRepository.findById(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerProprietaire(99L, "sig", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("signerProprietaire() — email proprio incorrect → KupangaBusinessException 403")
    void signerProprietaire_wrongEmail_throwsException() {
        when(contratRepository.findById(1L)).thenReturn(Optional.of(contrat));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerProprietaire(1L, "sig", "inconnu@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(emailService, never()).envoyerInvitationSignature(any(Contrat.class), any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = StatutContrat.class, names = {"SIGNE", "ANNULE"})
    @DisplayName("signerProprietaire() — contrat signé ou annulé → 409, rien de modifié (B6)")
    void signerProprietaire_contratFige_throwsConflict(StatutContrat statut) {
        contrat.setStatut(statut);
        contrat.setSignatureProprietaire("sig-origine");
        when(contratRepository.findById(1L)).thenReturn(Optional.of(contrat));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerProprietaire(1L, "sig-nouvelle", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(contrat.getStatut()).isEqualTo(statut);
        assertThat(contrat.getSignatureProprietaire()).isEqualTo("sig-origine");
        assertThat(contrat.getTokenSignature()).isEqualTo("token-valide"); // lien en cours non remplacé
        verify(contratRepository, never()).save(any());
        verify(contratRepository, never()).saveAndFlush(any());
        verify(emailService, never()).envoyerInvitationSignature(any(Contrat.class), any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = StatutContrat.class, names = {"EN_ATTENTE_SIGNATURE_LOCATAIRE", "EXPIRE"})
    @DisplayName("signerProprietaire() — en attente du locataire ou expiré → nouvelle invitation (B6)")
    void signerProprietaire_relance_autorisee(StatutContrat statut) {
        contrat.setStatut(statut);
        when(contratRepository.findById(1L)).thenReturn(Optional.of(contrat));
        when(contratPdfService.genererEtUploaderPdf(contrat)).thenReturn("cle.pdf");

        contratService.signerProprietaire(1L, "sig-base64", proprietaire.getMail());

        assertThat(contrat.getStatut()).isEqualTo(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        verify(emailService).envoyerInvitationSignature(any(Contrat.class), anyString());
    }

    // ══════════════════════════════════════════════════════════════
    // signerLocataire
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("signerLocataire() — contrat modifié en parallèle (verrou optimiste) → conflit, ni e-mail ni notification (B6)")
    void signerLocataire_conflitDeVersion_aucunEnvoi() {
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        when(contratPdfService.genererEtUploaderPdf(contrat)).thenReturn("cle.pdf");
        when(contratRepository.saveAndFlush(contrat))
                .thenThrow(new ObjectOptimisticLockingFailureException(Contrat.class, 1L));

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> contratService.signerLocataire("token-valide", "sig"));

        verify(emailService, never()).envoyerConfirmationContratSigne(any());
        verifyNoInteractions(notificationService);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = StatutContrat.class, names = {"EN_ATTENTE_SIGNATURE_PROPRIO", "SIGNE", "ANNULE", "BROUILLON"})
    @DisplayName("signerLocataire() — contrat pas en attente du locataire → 409, rien d'enregistré (B6)")
    void signerLocataire_wrongStatut_throwsConflict(StatutContrat statut) {
        contrat.setStatut(statut);
        contrat.setTokenSignature("token-valide");
        contrat.setTokenExpiration(LocalDateTime.now().plusHours(1));
        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerLocataire("token-valide", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(contrat.getStatut()).isEqualTo(statut);
        assertThat(contrat.getSignatureLocataire()).isNull();
        verify(contratRepository, never()).save(any());
        verify(contratRepository, never()).saveAndFlush(any());
        verify(emailService, never()).envoyerConfirmationContratSigne(any());
    }

    @Test
    @DisplayName("signerLocataire() — succès : contrat signé, PDF final, confirmation email")
    void signerLocataire_success() {
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        when(contratPdfService.genererEtUploaderPdf(contrat)).thenReturn("http://minio/final.pdf");
        when(contratRepository.save(contrat)).thenReturn(contrat);
        doNothing().when(emailService).envoyerConfirmationContratSigne(contrat);

        assertDoesNotThrow(() -> contratService.signerLocataire("token-valide", "sig-locataire"));

        assertThat(contrat.getSignatureLocataire()).isEqualTo("sig-locataire");
        assertThat(contrat.getStatut()).isEqualTo(StatutContrat.SIGNE);
        assertThat(contrat.getTokenSignature()).isNull();
        assertThat(contrat.getClePdf()).isEqualTo("http://minio/final.pdf");
        verify(emailService).envoyerConfirmationContratSigne(contrat);
        verify(notificationService, times(2)).saveAndSend(
                any(User.class), eq(NotificationType.CONTRAT_SIGNE),
                anyString(), anyString(), isNull(), eq(contrat.getId()));
    }

    @Test
    @DisplayName("signerLocataire() — token introuvable → KupangaBusinessException 404 (B7)")
    void signerLocataire_tokenNotFound_throwsException() {
        when(contratRepository.findByTokenSignature("mauvais")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerLocataire("mauvais", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("signerLocataire() — token expiré → passage à EXPIRE + KupangaBusinessException 410 (B7)")
    void signerLocataire_expired_setsExpiredAndThrows() {
        contrat.setTokenExpiration(LocalDateTime.now().minusSeconds(1));

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        when(contratRepository.save(contrat)).thenReturn(contrat);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerLocataire("token-valide", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.GONE);
        // EXPIRE enregistré dans sa propre transaction (sinon annulé avec le refus)
        verify(contratRepository).marquerExpire(contrat.getId(), contrat.getVersion());
        verify(emailService, never()).envoyerConfirmationContratSigne(any());
    }

    // ══════════════════════════════════════════════════════════════
    // Fixture
    // ══════════════════════════════════════════════════════════════

    private ContratFormDTO buildValidContratFormDTO() {
        return ContratFormDTO.builder()
                .bienId(1L)
                .emailLocataire(locataire.getMail())
                .dateDebut(LocalDate.now().plusDays(1))
                .dateFin(LocalDate.now().plusMonths(12))
                .dureeBailMois(12)
                .loyerMensuel(new BigDecimal("850.0"))
                .chargesMensuelles(new BigDecimal("50.0"))
                .depotGarantie(new BigDecimal("1700.0"))
                .build();
    }

    @Test
    @DisplayName("B12 : creerContrat() — bien archivé → 409, aucun document créé")
    void creerContrat_bienArchive_409() {
        ContratFormDTO dto = buildValidContratFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail())).thenReturn(bien);
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierBienActif(bien);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.creerContrat(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(bienService, never()).verifierLocataireDuBien(any(), any());
        verify(contratRepository, never()).save(any());
        verify(contratRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("B12 : signerProprietaire() — bien archivé ou partie anonymisée → 409, ni PDF, ni e-mail, ni notification")
    void signerProprietaire_documentFige_409() {
        when(contratRepository.findById(1L)).thenReturn(Optional.of(contrat));
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierDocumentModifiable(any(), any(), any());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerProprietaire(1L, "sig-base64", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(contratPdfService, never()).genererEtUploaderPdf(any());
        verify(contratRepository, never()).saveAndFlush(any());
        verify(emailService, never()).envoyerInvitationSignature(any(Contrat.class), anyString());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("B12 : signerLocataire() — bien archivé ou partie anonymisée → 409, contrat non signé")
    void signerLocataire_documentFige_409() {
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierDocumentModifiable(any(), any(), any());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerLocataire("token-valide", "sig-locataire"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(contrat.getStatut()).isEqualTo(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        verify(contratPdfService, never()).genererEtUploaderPdf(any());
        verify(emailService, never()).envoyerConfirmationContratSigne(any());
    }
}
