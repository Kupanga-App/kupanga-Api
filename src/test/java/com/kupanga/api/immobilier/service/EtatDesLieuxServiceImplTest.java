package com.kupanga.api.immobilier.service;

import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.EtatDesLieuxFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.EtatDesLieuxMapper;
import com.kupanga.api.immobilier.pdf.EtatDesLieuxPdfService;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.service.impl.EtatDesLieuxServiceImpl;
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

    @Mock private EtatDesLieuxRepository edlRepository;
    @Mock private EtatDesLieuxPdfService  edlPdfService;
    @Mock private EmailService            emailService;
    @Mock private EtatDesLieuxMapper      edlMapper;
    @Mock private BienService             bienService;
    @Mock private UserService             userService;

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

        bien = Bien.builder()
                .id(1L)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .build();

        edl = EtatDesLieux.builder()
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
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L)).thenReturn(bien);
        when(edlRepository.save(any(EtatDesLieux.class))).thenReturn(edl);
        when(edlPdfService.genererEtUploaderPdf(edl)).thenReturn("http://minio/edl.pdf");

        assertDoesNotThrow(() -> edlService.creerEtatDesLieux(dto, proprietaire.getMail()));

        verify(edlRepository, times(2)).save(any(EtatDesLieux.class));
        verify(edlPdfService).genererEtUploaderPdf(edl);
    }

    @Test
    @DisplayName("creerEtatDesLieux() — bien introuvable → KupangaBusinessException 404")
    void creerEtatDesLieux_bienNotFound_throwsException() {
        EtatDesLieuxFormDTO dto = buildValidEdlFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.creerEtatDesLieux(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(edlRepository, never()).save(any());
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
        assertThat(edl.getUrlPdf()).isEqualTo("http://minio/edl-signed.pdf");
        verify(emailService).envoyerInvitationSignature(any(EtatDesLieux.class), anyString());
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
    @DisplayName("signerProprietaire() — email incorrect → KupangaBusinessException 401")
    void signerProprietaire_wrongEmail_throwsException() {
        when(edlRepository.findWithAllRelations(1L)).thenReturn(Optional.of(edl));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerProprietaire(1L, "sig", "inconnu@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
        assertThat(edl.getUrlPdf()).isEqualTo("http://minio/edl-final.pdf");
        verify(emailService).envoyerConfirmationEdlSigne(edl);
    }

    @Test
    @DisplayName("signerLocataire() — token introuvable → KupangaBusinessException 401")
    void signerLocataire_tokenNotFound_throwsException() {
        when(edlRepository.findByTokenSignature("mauvais")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerLocataire("mauvais", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("signerLocataire() — token expiré → statut EXPIRE + KupangaBusinessException 401")
    void signerLocataire_expired_setsExpiredAndThrows() {
        edl.setTokenExpiration(LocalDateTime.now().minusMinutes(1));

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));
        when(edlRepository.save(edl)).thenReturn(edl);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerLocataire("token-edl", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(edl.getStatut()).isEqualTo(StatutEdl.EXPIRE);
        verify(emailService, never()).envoyerConfirmationEdlSigne(any());
    }

    @Test
    @DisplayName("signerLocataire() — statut != EN_ATTENTE_SIGNATURE_LOCATAIRE → KupangaBusinessException 400")
    void signerLocataire_wrongStatut_throwsBadRequest() {
        edl.setStatut(StatutEdl.SIGNE);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.signerLocataire("token-edl", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
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
    @DisplayName("getEdlParToken() — token introuvable → KupangaBusinessException 401")
    void getEdlParToken_tokenNotFound_throwsException() {
        when(edlRepository.findByTokenSignature("inconnu")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.getEdlParToken("inconnu"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("getEdlParToken() — token expiré → statut EXPIRE + KupangaBusinessException 401")
    void getEdlParToken_expired_setsExpiredAndThrows() {
        edl.setTokenExpiration(LocalDateTime.now().minusMinutes(5));
        edl.setStatut(StatutEdl.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));
        when(edlRepository.save(edl)).thenReturn(edl);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.getEdlParToken("token-edl"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(edl.getStatut()).isEqualTo(StatutEdl.EXPIRE);
    }

    @Test
    @DisplayName("getEdlParToken() — statut != EN_ATTENTE_SIGNATURE_LOCATAIRE → KupangaBusinessException 400")
    void getEdlParToken_wrongStatut_throwsBadRequest() {
        edl.setStatut(StatutEdl.SIGNE);

        when(edlRepository.findByTokenSignature("token-edl")).thenReturn(Optional.of(edl));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> edlService.getEdlParToken("token-edl"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
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
}
