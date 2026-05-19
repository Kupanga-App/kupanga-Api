package com.kupanga.api.immobilier.service;

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

@DisplayName("Tests unitaires — QuittanceServiceImpl")
class QuittanceServiceImplTest {

    @Mock private QuittanceRepository quittanceRepository;
    @Mock private QuittancePdfService  quittancePdfService;
    @Mock private QuittanceMapper      quittanceMapper;
    @Mock private BienService          bienService;
    @Mock private UserService          userService;
    @Mock private ContratRepository    contratRepository;
    @Mock private EmailService         emailService;

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

        bien = Bien.builder()
                .id(1L)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .build();

        contrat = Contrat.builder()
                .id(10L)
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .loyerMensuel(850.0)
                .chargesMensuelles(50.0)
                .statut(StatutContrat.SIGNE)
                .build();

        quittance = Quittance.builder()
                .id(1L)
                .bien(bien)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .mois("janvier")
                .annee(2025)
                .loyerMensuel(850.0)
                .chargesMensuelles(50.0)
                .montantTotal(900.0)
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
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L)).thenReturn(bien);
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
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L)).thenReturn(bien);
        when(quittanceRepository.findByBienIdAndMoisAndAnnee(1L, "janvier", 2025))
                .thenReturn(Optional.empty());
        when(contratRepository.findById(10L)).thenReturn(Optional.of(contrat));
        when(quittanceRepository.save(any(Quittance.class))).thenReturn(quittance);
        when(quittancePdfService.genererEtUploaderPdf(quittance)).thenReturn("http://minio/q.pdf");

        assertDoesNotThrow(() -> quittanceService.creerQuittance(dto, proprietaire.getMail()));

        verify(contratRepository).findById(10L);
    }

    @Test
    @DisplayName("creerQuittance() — doublon mois/année → KupangaBusinessException 409")
    void creerQuittance_duplicate_throwsConflict() {
        QuittanceFormDTO dto = buildValidFormDTO(null);

        when(userService.getUserByEmail(proprietaire.getMail())).thenReturn(proprietaire);
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L)).thenReturn(bien);
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
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L)).thenReturn(bien);
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
        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienService.findWithAllProperties(1L)).thenReturn(bien);
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
        assertThat(quittance.getUrlPdf()).isEqualTo("http://minio/signed.pdf");
        verify(emailService).envoyerQuittance(quittance);
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
    @DisplayName("marquerPayee() — email proprio incorrect → KupangaBusinessException 401")
    void marquerPayee_wrongEmail_throwsUnauthorized() {
        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.marquerPayee(1L, "sig", "inconnu@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
    @DisplayName("getQuittancesParLocataire() — retourne la liste des quittances du locataire")
    void getQuittancesParLocataire_returnsList() {
        QuittanceDTO dto = new QuittanceDTO();
        dto.setId(1L);

        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(quittanceRepository.findByProprietaireId(locataire.getId())).thenReturn(List.of(quittance));
        when(quittanceMapper.toDTO(quittance)).thenReturn(dto);

        List<QuittanceDTO> result = quittanceService.getQuittancesParLocataire(locataire.getMail());

        assertThat(result).hasSize(1);
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
    @DisplayName("getQuittanceById() — utilisateur non autorisé → KupangaBusinessException 401")
    void getQuittanceById_unauthorized_throwsException() {
        when(quittanceRepository.findWithAllRelations(1L)).thenReturn(Optional.of(quittance));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> quittanceService.getQuittanceById(1L, "tiers@test.com"));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
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

    private QuittanceFormDTO buildValidFormDTO(Long contratId) {
        QuittanceFormDTO dto = new QuittanceFormDTO();
        dto.setBienId(1L);
        dto.setEmailLocataire(locataire.getMail());
        dto.setContratId(contratId);
        dto.setMois("janvier");
        dto.setAnnee(2025);
        dto.setLoyerMensuel(850.0);
        dto.setChargesMensuelles(50.0);
        dto.setDateEcheance(LocalDate.of(2025, 1, 5));
        return dto;
    }
}
