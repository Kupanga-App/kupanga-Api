package com.kupanga.api.immobilier.service;

import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.EtatDesLieuxFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.EtatDesLieuxMapper;
import com.kupanga.api.immobilier.pdf.EtatDesLieuxPdfService;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.service.impl.EtatDesLieuxServiceImpl;
import com.kupanga.api.notification.enums.NotificationType;
import com.kupanga.api.notification.service.NotificationService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — EtatDesLieuxServiceImpl")
class EtatDesLieuxServiceImplTest {

    @Mock private EtatDesLieuxRepository  edlRepository;
    @Mock private EtatDesLieuxPdfService  edlPdfService;
    @Mock private EmailService            emailService;
    @Mock private EtatDesLieuxMapper      edlMapper;
    @Mock private BienService             bienService;
    @Mock private UserService             userService;
    @Mock private NotificationService     notificationService;
    /** J5 : vrai registre (version du modèle figée sur l'EDL). */
    @Spy  private JuridictionRegistry     juridictionRegistry = JuridictionsDeTest.registre();

    @InjectMocks
    private EtatDesLieuxServiceImpl edlService;

    private User proprietaire;
    private User locataire;
    private Bien bien;
    private EtatDesLieux edl;

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

        bien = Bien.builder().pays(Pays.FR)
                .id(1L)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .build();

        edl = EtatDesLieux.builder().pays(Pays.FR).modeleVersion("fr-v1")
                .id(1L)
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .type(TypeEtat.ENTREE)
                .dateRealisation(LocalDate.now())
                .statut(StatutEdl.EN_ATTENTE_SIGNATURE_PROPRIO)
                .tokenSignature("token-edl")
                .tokenExpiration(LocalDateTime.now().plusHours(48))
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // creerEtatDesLieux
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("creerEtatDesLieux() — succès : EDL créé, PDF généré et sauvegardé")
    void creerEtatDesLieux_success_savesEdlWithPdf() {
        EtatDesLieuxFormDTO dto = buildValidEdlFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(edlRepository.save(any(EtatDesLieux.class))).thenReturn(edl);
        when(edlPdfService.genererEtUploaderPdf(edl)).thenReturn("http://minio/edl.pdf");

        assertDoesNotThrow(() -> edlService.creerEtatDesLieux(dto, proprietaire.getMail()));

        verify(edlRepository, times(2)).save(any(EtatDesLieux.class));
        verify(edlPdfService).genererEtUploaderPdf(edl);
    }

    @Test
    @DisplayName("J5 : creerEtatDesLieux() — pays du bien et version courante du modèle figés sur l'EDL")
    void creerEtatDesLieux_figeJuridiction() {
        bien.setPays(Pays.CD);
        EtatDesLieuxFormDTO dto = buildValidEdlFormDTO();
        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(edlRepository.save(any(EtatDesLieux.class))).thenAnswer(inv -> inv.getArgument(0));

        edlService.creerEtatDesLieux(dto, proprietaire.getMail());

        ArgumentCaptor<EtatDesLieux> captor = ArgumentCaptor.forClass(EtatDesLieux.class);
        verify(edlRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPays()).isEqualTo(Pays.CD);
        assertThat(captor.getAllValues().get(0).getModeleVersion()).isEqualTo("cd-v1");
    }

    @Test
    @DisplayName("creerEtatDesLieux() — bien introuvable → KupangaBusinessException 404")
    void creerEtatDesLieux_bienNotFound_throwsException() {
        EtatDesLieuxFormDTO dto = buildValidEdlFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any()))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.creerEtatDesLieux(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(edlRepository, never()).save(any());
        verify(edlRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("creerEtatDesLieux() — bien d'un autre propriétaire → 403, aucun EDL (P0-5)")
    void creerEtatDesLieux_bienDAutrui_throwsForbidden() {
        EtatDesLieuxFormDTO dto = buildValidEdlFormDTO();

        when(userService.getUserByEmail("autre@test.com")).thenReturn(User.builder().id(9L).mail("autre@test.com").build());
        when(bienService.verifierProprietaire(eq(1L), eq("autre@test.com")))
                .thenThrow(new KupangaBusinessException("Accès refusé", HttpStatus.FORBIDDEN));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.creerEtatDesLieux(dto, "autre@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(edlRepository, never()).save(any());
        verify(edlRepository, never()).saveAndFlush(any());
        verify(edlPdfService, never()).genererEtUploaderPdf(any());
    }

    @Test
    @DisplayName("creerEtatDesLieux() — e-mail locataire différent du locataire du bien → 400 (P0-5)")
    void creerEtatDesLieux_mauvaisLocataire_throwsBadRequest() {
        EtatDesLieuxFormDTO dto = buildValidEdlFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any()))
                .thenThrow(new KupangaBusinessException("Mauvais locataire", HttpStatus.BAD_REQUEST));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.creerEtatDesLieux(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(edlRepository, never()).save(any());
        verify(edlRepository, never()).saveAndFlush(any());
        verify(edlPdfService, never()).genererEtUploaderPdf(any());
    }

    // ══════════════════════════════════════════════════════════════
    // signerProprietaire
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("signerProprietaire() — succès : EDL signé, token généré, invitation envoyée")
    void signerProprietaire_success() {
        when(edlRepository.findWithAllRelations(1L)).thenReturn(Optional.of(edl));
        when(edlPdfService.genererEtUploaderPdf(edl)).thenReturn("http://minio/edl-signed.pdf");
        when(edlRepository.save(edl)).thenReturn(edl);
        doNothing().when(emailService).envoyerInvitationSignature(any(EtatDesLieux.class), anyString());

        assertDoesNotThrow(() ->
                edlService.signerProprietaire(1L, "sig-proprio", proprietaire.getMail()));

        assertThat(edl.getSignatureProprietaire()).isEqualTo("sig-proprio");
        assertThat(edl.getStatut()).isEqualTo(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        assertThat(edl.getTokenSignature()).isNotNull();
        assertThat(edl.getClePdf()).isEqualTo("http://minio/edl-signed.pdf");
        verify(emailService).envoyerInvitationSignature(any(EtatDesLieux.class), anyString());
        verify(notificationService).saveAndSend(
                eq(locataire), eq(NotificationType.INVITATION_SIGNATURE_EDL),
                anyString(), anyString(), anyString(), eq(edl.getId()));
    }

    @Test
    @DisplayName("signerProprietaire() — EDL introuvable → KupangaBusinessException 404")
    void signerProprietaire_edlNotFound_throwsException() {
        when(edlRepository.findWithAllRelations(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerProprietaire(99L, "sig", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("signerProprietaire() — email incorrect → KupangaBusinessException 403")
    void signerProprietaire_wrongEmail_throwsException() {
        when(edlRepository.findWithAllRelations(1L)).thenReturn(Optional.of(edl));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerProprietaire(1L, "sig", "inconnu@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(emailService, never()).envoyerInvitationSignature(any(EtatDesLieux.class), any());
    }

    // ══════════════════════════════════════════════════════════════
    // signerLocataire
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("signerLocataire() — succès : EDL finalisé SIGNE, token invalidé, email envoyé")
    void signerLocataire_success() {
        edl.setStatut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));
        when(edlPdfService.genererEtUploaderPdf(edl)).thenReturn("http://minio/edl-final.pdf");
        when(edlRepository.save(edl)).thenReturn(edl);
        doNothing().when(emailService).envoyerConfirmationEdlSigne(edl);

        assertDoesNotThrow(() -> edlService.signerLocataire("token-edl", "sig-locataire"));

        assertThat(edl.getSignatureLocataire()).isEqualTo("sig-locataire");
        assertThat(edl.getStatut()).isEqualTo(StatutEdl.SIGNE);
        assertThat(edl.getTokenSignature()).isNull();
        assertThat(edl.getTokenExpiration()).isNull();
        assertThat(edl.getClePdf()).isEqualTo("http://minio/edl-final.pdf");
        verify(emailService).envoyerConfirmationEdlSigne(edl);
        verify(notificationService, times(2)).saveAndSend(
                any(User.class), eq(NotificationType.EDL_SIGNE),
                anyString(), anyString(), isNull(), eq(edl.getId()));
    }

    @Test
    @DisplayName("signerLocataire() — token introuvable → KupangaBusinessException 404 (B7)")
    void signerLocataire_tokenNotFound_throwsException() {
        when(edlRepository.findByTokenSignature("mauvais")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerLocataire("mauvais", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("signerLocataire() — token expiré → passage à EXPIRE + KupangaBusinessException 410 (B7)")
    void signerLocataire_expired_setsExpiredAndThrows() {
        edl.setTokenExpiration(LocalDateTime.now().minusMinutes(1));

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));
        when(edlRepository.save(edl)).thenReturn(edl);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerLocataire("token-edl", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.GONE);
        // EXPIRE enregistré dans sa propre transaction (sinon annulé avec le refus)
        verify(edlRepository).marquerExpire(edl.getId(), edl.getVersion());
        verify(emailService, never()).envoyerConfirmationEdlSigne(any());
    }

    @Test
    @DisplayName("signerLocataire() — statut != EN_ATTENTE_SIGNATURE_LOCATAIRE → 409, rien d'enregistré (B6)")
    void signerLocataire_wrongStatut_throwsConflict() {
        edl.setStatut(StatutEdl.SIGNE);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerLocataire("token-edl", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).doesNotContain("SIGNE");
        verify(edlRepository, never()).save(any());
        verify(edlRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("signerProprietaire() — EDL déjà SIGNE → 409, statut et signatures inchangés (B6)")
    void signerProprietaire_dejaSigne_throwsConflict() {
        edl.setStatut(StatutEdl.SIGNE);
        edl.setSignatureProprietaire("sig-origine");
        when(edlRepository.findWithAllRelations(1L)).thenReturn(Optional.of(edl));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerProprietaire(1L, "sig-nouvelle", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(edl.getStatut()).isEqualTo(StatutEdl.SIGNE);
        assertThat(edl.getSignatureProprietaire()).isEqualTo("sig-origine");
        verify(edlRepository, never()).save(any());
        verify(edlRepository, never()).saveAndFlush(any());
        verify(edlPdfService, never()).genererEtUploaderPdf(any());
    }

    // ══════════════════════════════════════════════════════════════
    // getEdlParToken
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getEdlParToken() — token valide, statut correct → EtatDesLieuxDTO retourné")
    void getEdlParToken_valid_returnsDTO() {
        edl.setStatut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);
        EtatDesLieuxDTO dto = new EtatDesLieuxDTO();
        dto.setId(1L);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));
        when(edlMapper.toDTO(edl)).thenReturn(dto);

        EtatDesLieuxDTO result = edlService.getEdlParToken("token-edl");

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getEdlParToken() — token introuvable → KupangaBusinessException 404 (B7)")
    void getEdlParToken_tokenNotFound_throwsException() {
        when(edlRepository.findByTokenSignature("inconnu")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.getEdlParToken("inconnu"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getEdlParToken() — token expiré → passage à EXPIRE + KupangaBusinessException 410 (B7)")
    void getEdlParToken_expired_setsExpiredAndThrows() {
        edl.setTokenExpiration(LocalDateTime.now().minusMinutes(5));
        edl.setStatut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));
        when(edlRepository.save(edl)).thenReturn(edl);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.getEdlParToken("token-edl"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.GONE);
        // EXPIRE enregistré dans sa propre transaction (sinon annulé avec le refus)
        verify(edlRepository).marquerExpire(edl.getId(), edl.getVersion());
    }

    @Test
    @DisplayName("getEdlParToken() — statut != EN_ATTENTE_SIGNATURE_LOCATAIRE → 409 (B7)")
    void getEdlParToken_wrongStatut_throwsConflict() {
        edl.setStatut(StatutEdl.SIGNE);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.getEdlParToken("token-edl"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ══════════════════════════════════════════════════════════════
    // Fixture
    // ══════════════════════════════════════════════════════════════

    private EtatDesLieuxFormDTO buildValidEdlFormDTO() {
        EtatDesLieuxFormDTO dto = new EtatDesLieuxFormDTO();
        dto.setBienId(1L);
        dto.setEmailLocataire(locataire.getMail());
        dto.setType(TypeEtat.ENTREE);
        dto.setDateRealisation(LocalDate.now());
        return dto;
    }

    @Test
    @DisplayName("B12 : creerEtatDesLieux() — bien archivé → 409, aucun document créé")
    void creerEtatDesLieux_bienArchive_409() {
        EtatDesLieuxFormDTO dto = buildValidEdlFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail())).thenReturn(bien);
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierBienActif(bien);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.creerEtatDesLieux(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(bienService, never()).verifierLocataireDuBien(any(), any());
        verify(edlRepository, never()).save(any());
        verify(edlRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("B12 : signerProprietaire() — bien archivé ou partie anonymisée → 409, ni PDF ni invitation")
    void signerProprietaire_documentFige_409() {
        when(edlRepository.findWithAllRelations(1L)).thenReturn(Optional.of(edl));
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierDocumentModifiable(any(), any(), any());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerProprietaire(1L, "sig-proprio", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(edlPdfService, never()).genererEtUploaderPdf(any());
        verify(emailService, never()).envoyerInvitationSignature(any(EtatDesLieux.class), anyString());
        verifyNoInteractions(notificationService);
    }
}
