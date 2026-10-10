package com.kupanga.api.immobilier.service;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.email.service.EmailService;
import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.QuittanceFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.QuittanceMapper;
import com.kupanga.api.immobilier.pdf.QuittancePdfService;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.service.impl.QuittanceServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;

@DisplayName("Tests unitaires — QuittanceServiceImpl")
class QuittanceServiceImplTest {

    @Mock private QuittanceRepository  quittanceRepository;
    @Mock private QuittancePdfService  quittancePdfService;
    @Mock private QuittanceMapper      quittanceMapper;
    @Mock private BienService          bienService;
    @Mock private UserService          userService;
    @Mock private ContratRepository    contratRepository;
    @Mock private EmailService         emailService;
    @Mock private NotificationService  notificationService;
    /** J3 : vrai registre (profils et plafonds de application.yml). */
    @Spy  private JuridictionRegistry  juridictionRegistry = JuridictionsDeTest.registre();

    @InjectMocks
    private QuittanceServiceImpl quittanceService;

    private User proprietaire;
    private User locataire;
    private Bien bien;
    private Quittance quittance;
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
                .id(10L)
                .pays(Pays.FR).devise(Devise.EUR).modeleVersion("fr-v1")
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .loyerMensuel(new BigDecimal("850.0"))
                .chargesMensuelles(new BigDecimal("50.0"))
                .statut(StatutContrat.SIGNE)
                .build();

        quittance = Quittance.builder()
                .id(1L)
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .mois("janvier")
                .annee(2025)
                .loyerMensuel(new BigDecimal("850.0"))
                .chargesMensuelles(new BigDecimal("50.0"))
                .montantTotal(new BigDecimal("900.0"))
                .statut(StatutQuittance.EN_ATTENTE)
                .dateEcheance(LocalDate.of(2025, 1, 5))
                .build();
    }

    // ══════════════════════════════════════════════════════════════
    // creerQuittance
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("creerQuittance() — succès sans contrat : loyer fourni directement")
    void creerQuittance_success_withoutContrat() {
        QuittanceFormDTO dto = buildValidFormDTO(null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025))
                .thenReturn(Optional.empty());
        when(quittanceRepository.save(any(Quittance.class))).thenReturn(quittance);
        when(quittancePdfService.genererEtUploaderPdf(quittance)).thenReturn("http://minio/quittance.pdf");

        assertDoesNotThrow(() -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        verify(quittanceRepository, times(2)).save(any(Quittance.class));
        verify(quittancePdfService).genererEtUploaderPdf(quittance);
    }

    @Test
    @DisplayName("creerQuittance() — succès avec contrat : loyer récupéré depuis le contrat")
    void creerQuittance_success_withContrat() {
        QuittanceFormDTO dto = buildValidFormDTO(10L);
        dto.setLoyerMensuel(null);
        dto.setChargesMensuelles(null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025))
                .thenReturn(Optional.empty());
        when(contratRepository.findById(10L)).thenReturn(Optional.of(contrat));
        when(quittanceRepository.save(any(Quittance.class))).thenReturn(quittance);
        when(quittancePdfService.genererEtUploaderPdf(quittance)).thenReturn("http://minio/q.pdf");

        assertDoesNotThrow(() -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        verify(contratRepository).findById(10L);
    }

    @Test
    @DisplayName("J3 : creerQuittance() avec contrat — juridiction figée du bail copiée (pays, devise, modèle), même si le bien a changé de devise")
    void creerQuittance_avecContrat_copieJuridictionDuBail() {
        bien.setPays(Pays.CD);
        bien.setDevise(Devise.CDF);
        contrat.setPays(Pays.CD);
        contrat.setDevise(Devise.USD);
        contrat.setModeleVersion("cd-v0"); // ancienne version du modèle, figée sur le bail
        QuittanceFormDTO dto = buildValidFormDTO(10L);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025)).thenReturn(Optional.empty());
        when(contratRepository.findById(10L)).thenReturn(Optional.of(contrat));
        when(quittanceRepository.save(any(Quittance.class))).thenAnswer(inv -> inv.getArgument(0));

        quittanceService.creerQuittance(dto, proprietaire.getMail());

        ArgumentCaptor<Quittance> captor = ArgumentCaptor.forClass(Quittance.class);
        verify(quittanceRepository, atLeastOnce()).save(captor.capture());
        Quittance creee = captor.getAllValues().get(0);
        assertThat(creee.getPays()).isEqualTo(Pays.CD);
        assertThat(creee.getDevise()).isEqualTo(Devise.USD);
        assertThat(creee.getModeleVersion()).isEqualTo("cd-v0");
        assertThat(creee.getMontantTotal()).isEqualByComparingTo("900.0");
    }

    @Test
    @DisplayName("J3 : creerQuittance() sans contrat — devise du bien, version du modèle du pays")
    void creerQuittance_sansContrat_juridictionDuBien() {
        bien.setPays(Pays.CD);
        bien.setDevise(Devise.CDF);
        QuittanceFormDTO dto = buildValidFormDTO(null);
        dto.setLoyerMensuel(new BigDecimal("250000.50"));
        dto.setChargesMensuelles(new BigDecimal("0.25"));

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025)).thenReturn(Optional.empty());
        when(quittanceRepository.save(any(Quittance.class))).thenAnswer(inv -> inv.getArgument(0));

        quittanceService.creerQuittance(dto, proprietaire.getMail());

        ArgumentCaptor<Quittance> captor = ArgumentCaptor.forClass(Quittance.class);
        verify(quittanceRepository, atLeastOnce()).save(captor.capture());
        Quittance creee = captor.getAllValues().get(0);
        assertThat(creee.getPays()).isEqualTo(Pays.CD);
        assertThat(creee.getDevise()).isEqualTo(Devise.CDF);
        assertThat(creee.getModeleVersion()).isEqualTo("cd-v1");
        // B13 : somme exacte, sans erreur d'arrondi binaire
        assertThat(creee.getMontantTotal()).isEqualByComparingTo("250000.75");
    }

    @Test
    @DisplayName("C5 : creerQuittance() sans contrat — loyer au-dessus du plafond de la devise → 400, rien d'enregistré")
    void creerQuittance_sansContrat_plafondDepasse_400() {
        QuittanceFormDTO dto = buildValidFormDTO(null);
        dto.setLoyerMensuel(new BigDecimal("150000"));

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(quittanceRepository, never()).save(any());
        verifyNoInteractions(quittancePdfService);
    }

    @Test
    @DisplayName("creerQuittance() — doublon mois/année → KupangaBusinessException 409")
    void creerQuittance_duplicate_throwsConflict() {
        QuittanceFormDTO dto = buildValidFormDTO(null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025))
                .thenReturn(Optional.of(quittance));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(quittanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerQuittance() — sans contrat ni loyer → KupangaBusinessException 400")
    void creerQuittance_noContratNoLoyer_throwsBadRequest() {
        QuittanceFormDTO dto = buildValidFormDTO(null);
        dto.setLoyerMensuel(null);
        dto.setChargesMensuelles(null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025))
                .thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("creerQuittance() — contrat introuvable → KupangaBusinessException 404")
    void creerQuittance_contratNotFound_throwsException() {
        QuittanceFormDTO dto = buildValidFormDTO(99L);
        dto.setLoyerMensuel(null);
        dto.setChargesMensuelles(null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025))
                .thenReturn(Optional.empty());
        when(contratRepository.findById(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ══════════════════════════════════════════════════════════════
    // marquerPayee
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("marquerPayee() — succès : statut PAYEE, PDF régénéré, email envoyé")
    void marquerPayee_success() {
        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));
        when(quittancePdfService.genererEtUploaderPdf(quittance)).thenReturn("http://minio/signed.pdf");
        when(quittanceRepository.save(quittance)).thenReturn(quittance);
        doNothing().when(emailService).envoyerQuittance(quittance);

        assertDoesNotThrow(() ->
                quittanceService.marquerPayee(1L, "sig-proprio", proprietaire.getMail()));

        assertThat(quittance.getStatut()).isEqualTo(StatutQuittance.PAYEE);
        assertThat(quittance.getSignatureProprietaire()).isEqualTo("sig-proprio");
        assertThat(quittance.getClePdf()).isEqualTo("http://minio/signed.pdf");
        verify(emailService).envoyerQuittance(quittance);
        verify(notificationService).saveAndSend(
                eq(locataire), eq(NotificationType.QUITTANCE_DISPONIBLE),
                anyString(), anyString(), isNull(), eq(quittance.getId()));
    }

    @Test
    @DisplayName("marquerPayee() — quittance déjà payée → KupangaBusinessException 400")
    void marquerPayee_alreadyPaid_throwsBadRequest() {
        quittance.setStatut(StatutQuittance.PAYEE);

        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.marquerPayee(1L, "sig", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(quittancePdfService, never()).genererEtUploaderPdf(any());
    }

    @Test
    @DisplayName("marquerPayee() — email proprio incorrect → KupangaBusinessException 403")
    void marquerPayee_wrongEmail_throwsUnauthorized() {
        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.marquerPayee(1L, "sig", "inconnu@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ══════════════════════════════════════════════════════════════
    // getQuittancesParBien
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getQuittancesParBien() — retourne uniquement les quittances du propriétaire")
    void getQuittancesParBien_returnsFilteredList() {
        QuittanceDTO dto = new QuittanceDTO();
        dto.setId(1L);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(quittanceRepository.findByBienId(1L)).thenReturn(List.of(quittance));
        when(quittanceMapper.toDTO(quittance)).thenReturn(dto);

        List<QuittanceDTO> result = quittanceService.getQuittancesParBien(1L, proprietaire.getMail());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ══════════════════════════════════════════════════════════════
    // getQuittancesParLocataire
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getQuittancesParLocataire() — cherche par id du locataire, pas du propriétaire (B1)")
    void getQuittancesParLocataire_returnsList() {
        QuittanceDTO dto = new QuittanceDTO();
        dto.setId(1L);

        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(quittanceRepository.findByLocataireId(locataire.getId())).thenReturn(List.of(quittance));
        when(quittanceMapper.toDTO(quittance)).thenReturn(dto);

        List<QuittanceDTO> result = quittanceService.getQuittancesParLocataire(locataire.getMail());

        assertThat(result).hasSize(1);
        verify(quittanceRepository).findByLocataireId(locataire.getId());
    }

    // ══════════════════════════════════════════════════════════════
    // getQuittanceById
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getQuittanceById() — accès autorisé propriétaire → QuittanceDTO retourné")
    void getQuittanceById_accessByProprietaire_returnsDTO() {
        QuittanceDTO dto = new QuittanceDTO();
        dto.setId(1L);

        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));
        when(quittanceMapper.toDTO(quittance)).thenReturn(dto);

        QuittanceDTO result = quittanceService.getQuittanceById(1L, proprietaire.getMail());

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getQuittanceById() — accès autorisé locataire → QuittanceDTO retourné")
    void getQuittanceById_accessByLocataire_returnsDTO() {
        QuittanceDTO dto = new QuittanceDTO();
        dto.setId(1L);

        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));
        when(quittanceMapper.toDTO(quittance)).thenReturn(dto);

        QuittanceDTO result = quittanceService.getQuittanceById(1L, locataire.getMail());

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getQuittanceById() — utilisateur non autorisé → KupangaBusinessException 403")
    void getQuittanceById_unauthorized_throwsException() {
        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.getQuittanceById(1L, "tiers@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("getQuittanceById() — quittance introuvable → KupangaBusinessException 404")
    void getQuittanceById_notFound_throwsException() {
        when(quittanceRepository.findWithAllRelations(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.getQuittanceById(99L, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ══════════════════════════════════════════════════════════════
    // Fixture
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("creerQuittance() — contrat d'un autre bien → 400, aucune quittance (P0-5)")
    void creerQuittance_contratAutreBien_throwsBadRequest() {
        Bien autreBien = Bien.builder().pays(Pays.FR).id(2L).build();
        Contrat contratAutreBien = Contrat.builder().id(5L).bien(autreBien)
                .loyerMensuel(new BigDecimal("500.0")).chargesMensuelles(new BigDecimal("20.0")).build();
        QuittanceFormDTO dto = buildValidFormDTO(5L);

        when(userService.getUserByEmail(any())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any())).thenReturn(bien);
        when(bienService.verifierLocataireDuBien(eq(bien), any())).thenReturn(locataire);
        when(contratRepository.findById(5L)).thenReturn(Optional.of(contratAutreBien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(quittanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerQuittance() — bien d'un autre propriétaire → 403 (P0-5)")
    void creerQuittance_bienDAutrui_throwsForbidden() {
        QuittanceFormDTO dto = buildValidFormDTO(null);

        when(userService.getUserByEmail(any())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(eq(1L), any()))
                .thenThrow(new KupangaBusinessException("Accès refusé", HttpStatus.FORBIDDEN));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.creerQuittance(dto, "autre@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(quittanceRepository, never()).save(any());
    }

    private QuittanceFormDTO buildValidFormDTO(Long contratId) {
        QuittanceFormDTO dto = new QuittanceFormDTO();
        dto.setBienId(1L);
        dto.setEmailLocataire(locataire.getMail());
        dto.setContratId(contratId);
        dto.setMois("janvier");
        dto.setAnnee(2025);
        dto.setLoyerMensuel(new BigDecimal("850.0"));
        dto.setChargesMensuelles(new BigDecimal("50.0"));
        dto.setDateEcheance(LocalDate.of(2025, 1, 5));
        return dto;
    }

    @Test
    @DisplayName("B12 : creerQuittance() — bien archivé → 409, aucun document créé")
    void creerQuittance_bienArchive_409() {
        QuittanceFormDTO dto = buildValidFormDTO(null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(bienService.verifierProprietaire(1L, proprietaire.getMail())).thenReturn(bien);
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierBienActif(bien);

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(bienService, never()).verifierLocataireDuBien(any(), any());
        verify(quittanceRepository, never()).save(any());
        verify(quittanceRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("B12 : marquerPayee() — bien archivé ou partie anonymisée → 409, PDF non régénéré")
    void marquerPayee_documentFige_409() {
        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));
        doThrow(new KupangaBusinessException("Ce bien est archivé", HttpStatus.CONFLICT))
                .when(bienService).verifierDocumentModifiable(any(), any(), any());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.marquerPayee(1L, "sig-proprio", proprietaire.getMail()));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(quittance.getStatut()).isNotEqualTo(StatutQuittance.PAYEE);
        verify(quittancePdfService, never()).genererEtUploaderPdf(any());
        verify(emailService, never()).envoyerQuittance(any());
    }
}
