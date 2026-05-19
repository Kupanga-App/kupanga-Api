package com.kupanga.api.immobilier.service;

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

@DisplayName("Tests unitaires — ContratServiceImpl")
class ContratServiceImplTest {

    @Mock private ContratRepository  contratRepository;
    @Mock private ContratPdfService  contratPdfService;
    @Mock private EmailService       emailService;
    @Mock private UserService        userService;
    @Mock private BienService        bienService;
    @Mock private ContratMapper      contratMapper;

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

        bien = Bien.builder()
                .id(1L)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .build();

        contrat = Contrat.builder()
                .id(1L)
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .loyerMensuel(850.0)
                .chargesMensuelles(50.0)
                .depotGarantie(1700.0)
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
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L)).thenReturn(bien);
        when(contratPdfService.genererEtUploaderPdf(any(Contrat.class))).thenReturn("http://minio/contrat.pdf");
        when(contratRepository.save(any(Contrat.class))).thenReturn(contrat);

        assertDoesNotThrow(() -> contratService.creerContrat(dto, proprietaire.getMail()));

        verify(contratPdfService).genererEtUploaderPdf(any(Contrat.class));
        verify(contratRepository).save(any(Contrat.class));
    }

    @Test
    @DisplayName("creerContrat() — bien introuvable → KupangaBusinessException 404")
    void creerContrat_bienNotFound_throwsException() {
        ContratFormDTO dto = buildValidContratFormDTO();

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L))
                .thenThrow(new KupangaBusinessException("Bien introuvable", HttpStatus.NOT_FOUND));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.creerContrat(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(contratRepository, never()).save(any());
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
    @DisplayName("getContratParToken() — token introuvable → KupangaBusinessException 401")
    void getContratParToken_tokenNotFound_throwsException() {
        when(contratRepository.findByTokenSignature("mauvais-token")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.getContratParToken("mauvais-token"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("getContratParToken() — token expiré → statut mis à EXPIRE + KupangaBusinessException 401")
    void getContratParToken_expired_setsExpiredAndThrows() {
        contrat.setTokenExpiration(LocalDateTime.now().minusMinutes(1));
        contrat.setStatut(StatutContrat.EN_ATTENTE_SIGNATURE_LOCATAIRE);

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        when(contratRepository.save(contrat)).thenReturn(contrat);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.getContratParToken("token-valide"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(contrat.getStatut()).isEqualTo(StatutContrat.EXPIRE);
        verify(contratRepository).save(contrat);
    }

    @Test
    @DisplayName("getContratParToken() — statut != EN_ATTENTE_SIGNATURE_LOCATAIRE → IllegalStateException")
    void getContratParToken_wrongStatut_throwsIllegalState() {
        contrat.setStatut(StatutContrat.SIGNE);

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));

        assertThrows(IllegalStateException.class,
                () -> contratService.getContratParToken("token-valide"));
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
        assertThat(contrat.getUrlPdf()).isEqualTo("http://minio/signed.pdf");
        verify(emailService).envoyerInvitationSignature(any(Contrat.class), anyString());
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
    @DisplayName("signerProprietaire() — email proprio incorrect → KupangaBusinessException 401")
    void signerProprietaire_wrongEmail_throwsException() {
        when(contratRepository.findById(1L)).thenReturn(Optional.of(contrat));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerProprietaire(1L, "sig", "inconnu@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(emailService, never()).envoyerInvitationSignature(any(Contrat.class), any());
    }

    // ══════════════════════════════════════════════════════════════
    // signerLocataire
    // ══════════════════════════════════════════════════════════════

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
        assertThat(contrat.getUrlPdf()).isEqualTo("http://minio/final.pdf");
        verify(emailService).envoyerConfirmationContratSigne(contrat);
    }

    @Test
    @DisplayName("signerLocataire() — token introuvable → KupangaBusinessException 401")
    void signerLocataire_tokenNotFound_throwsException() {
        when(contratRepository.findByTokenSignature("mauvais")).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerLocataire("mauvais", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("signerLocataire() — token expiré → statut EXPIRE + KupangaBusinessException 401")
    void signerLocataire_expired_setsExpiredAndThrows() {
        contrat.setTokenExpiration(LocalDateTime.now().minusSeconds(1));

        when(contratRepository.findByTokenSignature("token-valide")).thenReturn(Optional.of(contrat));
        when(contratRepository.save(contrat)).thenReturn(contrat);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> contratService.signerLocataire("token-valide", "sig"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(contrat.getStatut()).isEqualTo(StatutContrat.EXPIRE);
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
                .loyerMensuel(850.0)
                .chargesMensuelles(50.0)
                .depotGarantie(1700.0)
                .build();
    }
}
