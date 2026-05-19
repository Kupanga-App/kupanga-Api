package com.kupanga.api.immobilier.service;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.readDTO.EtatDesLieuxDTO;
import com.kupanga.api.immobilier.dto.readDTO.LocataireDashboardDTO;
import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.*;
import com.kupanga.api.immobilier.mapper.EtatDesLieuxMapper;
import com.kupanga.api.immobilier.mapper.QuittanceMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import com.kupanga.api.immobilier.service.impl.LocataireDashboardServiceImpl;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — LocataireDashboardServiceImpl")
class LocataireDashboardServiceImplTest {

    @Mock private UserService            userService;
    @Mock private BienRepository         bienRepository;
    @Mock private ContratRepository      contratRepository;
    @Mock private QuittanceRepository    quittanceRepository;
    @Mock private EtatDesLieuxRepository etatDesLieuxRepository;
    @Mock private QuittanceMapper        quittanceMapper;
    @Mock private EtatDesLieuxMapper     etatDesLieuxMapper;
    @Mock private Authentication         auth;

    @InjectMocks
    private LocataireDashboardServiceImpl dashboardService;

    private User locataire;
    private User proprietaire;
    private Bien bien;
    private Contrat contratActif;
    private Quittance quittance;
    private EtatDesLieux edl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        proprietaire = User.builder()
                .id(1L)
                .firstName("Jean")
                .lastName("Martin")
                .mail("proprio@test.com")
                .build();

        locataire = User.builder()
                .id(2L)
                .firstName("Alice")
                .lastName("Dupont")
                .mail("locataire@test.com")
                .build();

        bien = Bien.builder()
                .id(10L)
                .adresse("12 rue des Tests")
                .ville("Nantes")
                .codePostal("44000")
                .pays("France")
                .surfaceHabitable(65.0)
                .nombrePieces(3)
                .typeBien(TypeBien.APPARTEMENT)
                .meuble(true)
                .colocation(false)
                .proprietaire(proprietaire)
                .locataire(locataire)
                .images(new HashSet<>())
                .build();

        contratActif = Contrat.builder()
                .id(100L)
                .statut(StatutContrat.SIGNE)
                .dateDebut(LocalDate.of(2023, 9, 1))
                .dateFin(LocalDate.of(2024, 8, 31))
                .dureeBailMois(12)
                .loyerMensuel(850.0)
                .chargesMensuelles(80.0)
                .depotGarantie(1700.0)
                .build();

        quittance = Quittance.builder()
                .id(200L)
                .mois("Mars")
                .annee(2024)
                .loyerMensuel(850.0)
                .chargesMensuelles(80.0)
                .montantTotal(930.0)
                .statut(StatutQuittance.PAYEE)
                .createdAt(LocalDateTime.of(2024, 3, 1, 0, 0))
                .build();

        edl = EtatDesLieux.builder()
                .id(300L)
                .type(TypeEtat.ENTREE)
                .statut(StatutEdl.SIGNE)
                .dateRealisation(LocalDate.of(2023, 9, 1))
                .locataire(locataire)
                .build();

        when(auth.getName()).thenReturn(locataire.getMail());
    }

    // ══════════════════════════════════════════════════════════════
    // getDashboard — cas nominal
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getDashboard() — succès avec contrat actif : toutes les sections renseignées")
    void getDashboard_success_withContratActif() {

        QuittanceDTO quittanceDTO = new QuittanceDTO();
        EtatDesLieuxDTO edlDTO    = new EtatDesLieuxDTO();

        stubDependencies(List.of(contratActif), List.of(quittance), List.of(edl));
        when(quittanceMapper.toDTO(quittance)).thenReturn(quittanceDTO);
        when(etatDesLieuxMapper.toDTO(edl)).thenReturn(edlDTO);

        LocataireDashboardDTO result = dashboardService.getDashboard(auth, 10L);

        // ─── Locataire ────────────────────────────────────────────
        assertThat(result.getLocataire().getPrenom()).isEqualTo("Alice");
        assertThat(result.getLocataire().getNom()).isEqualTo("Dupont");
        assertThat(result.getLocataire().getInitiales()).isEqualTo("AD");

        // ─── Bien ─────────────────────────────────────────────────
        assertThat(result.getBien().getId()).isEqualTo(10L);
        assertThat(result.getBien().getAdresse()).isEqualTo("12 rue des Tests, 44000 Nantes");
        assertThat(result.getBien().getSurface()).isEqualTo(65.0);
        assertThat(result.getBien().getNbPieces()).isEqualTo(3);
        assertThat(result.getBien().getType()).isEqualTo("APPARTEMENT");
        assertThat(result.getBien().getNomProprietaire()).isEqualTo("Jean Martin");
        assertThat(result.getBien().getColocation()).isFalse();

        // ─── Contrat ──────────────────────────────────────────────
        assertThat(result.getContrat()).isNotNull();
        assertThat(result.getContrat().getId()).isEqualTo(100L);
        assertThat(result.getContrat().getType()).isEqualTo("MEUBLE");
        assertThat(result.getContrat().getLoyerMensuel()).isEqualTo(850.0);
        assertThat(result.getContrat().getCharges()).isEqualTo(80.0);
        assertThat(result.getContrat().getDepotGarantie()).isEqualTo(1700.0);
        assertThat(result.getContrat().getStatut()).isEqualTo(StatutContrat.SIGNE);
        assertThat(result.getContrat().getDureeTotale()).isEqualTo(12);
        assertThat(result.getContrat().getMoisEcoules()).isGreaterThanOrEqualTo(0);

        // ─── Statuts ──────────────────────────────────────────────
        assertThat(result.getStatuts().getDerniereQuittance()).isEqualTo("Mars 2024");
        assertThat(result.getStatuts().getDateFinContrat()).isEqualTo(LocalDate.of(2024, 8, 31));

        // ─── Collections ──────────────────────────────────────────
        assertThat(result.getQuittances()).hasSize(1);
        assertThat(result.getEtatsDesLieux()).hasSize(1);

        verify(quittanceMapper).toDTO(quittance);
        verify(etatDesLieuxMapper).toDTO(edl);
    }

    @Test
    @DisplayName("getDashboard() — aucun contrat SIGNE : section contrat null, statuts sans dateFinContrat")
    void getDashboard_noActiveContrat_contratIsNull() {

        Contrat brouillon = Contrat.builder()
                .id(99L)
                .statut(StatutContrat.BROUILLON)
                .build();

        stubDependencies(List.of(brouillon), List.of(quittance), List.of(edl));
        when(quittanceMapper.toDTO(any(Quittance.class))).thenReturn(new QuittanceDTO());
        when(etatDesLieuxMapper.toDTO(any(EtatDesLieux.class))).thenReturn(new EtatDesLieuxDTO());

        LocataireDashboardDTO result = dashboardService.getDashboard(auth, 10L);

        assertThat(result.getContrat()).isNull();
        assertThat(result.getStatuts().getDateFinContrat()).isNull();
    }

    @Test
    @DisplayName("getDashboard() — aucune quittance : liste vide, derniereQuittance null")
    void getDashboard_noQuittances_emptyListAndNullStatut() {

        stubDependencies(List.of(contratActif), Collections.emptyList(), Collections.emptyList());

        LocataireDashboardDTO result = dashboardService.getDashboard(auth, 10L);

        assertThat(result.getQuittances()).isEmpty();
        assertThat(result.getStatuts().getDerniereQuittance()).isNull();
        verifyNoInteractions(quittanceMapper);
    }

    @Test
    @DisplayName("getDashboard() — bien meuble=false : type contrat = NON_MEUBLE")
    void getDashboard_nonMeuble_typeIsNonMeuble() {

        bien.setMeuble(false);
        stubDependencies(List.of(contratActif), List.of(), List.of());

        LocataireDashboardDTO result = dashboardService.getDashboard(auth, 10L);

        assertThat(result.getContrat().getType()).isEqualTo("NON_MEUBLE");
    }

    @Test
    @DisplayName("getDashboard() — moisEcoules calculé correctement depuis dateDebut")
    void getDashboard_moisEcoules_calculatedFromDateDebut() {

        LocalDate dateDebut = LocalDate.now().minusMonths(7);
        contratActif = Contrat.builder()
                .id(100L)
                .statut(StatutContrat.SIGNE)
                .dateDebut(dateDebut)
                .dureeBailMois(12)
                .loyerMensuel(850.0)
                .chargesMensuelles(80.0)
                .depotGarantie(1700.0)
                .build();

        stubDependencies(List.of(contratActif), List.of(), List.of());

        LocataireDashboardDTO result = dashboardService.getDashboard(auth, 10L);

        assertThat(result.getContrat().getMoisEcoules())
                .isEqualTo((int) ChronoUnit.MONTHS.between(dateDebut, LocalDate.now()));
    }

    @Test
    @DisplayName("getDashboard() — photos du bien : URLs extraites des BienImage")
    void getDashboard_bienImages_urlsExtracted() {

        BienImage img1 = BienImage.builder().id(1L).url("https://cdn.kupanga.fr/photo1.jpg").build();
        BienImage img2 = BienImage.builder().id(2L).url("https://cdn.kupanga.fr/photo2.jpg").build();
        bien.setImages(Set.of(img1, img2));

        stubDependencies(List.of(), List.of(), List.of());

        LocataireDashboardDTO result = dashboardService.getDashboard(auth, 10L);

        assertThat(result.getBien().getPhotos())
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        "https://cdn.kupanga.fr/photo1.jpg",
                        "https://cdn.kupanga.fr/photo2.jpg"
                );
    }

    // ══════════════════════════════════════════════════════════════
    // getDashboard — erreurs
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getDashboard() — bien introuvable → KupangaBusinessException 404")
    void getDashboard_bienNotFound_throwsNotFound() {

        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienRepository.findById(99L)).thenReturn(Optional.empty());

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> dashboardService.getDashboard(auth, 99L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(contratRepository, quittanceRepository, etatDesLieuxRepository);
    }

    @Test
    @DisplayName("getDashboard() — bien sans locataire → KupangaBusinessException 403")
    void getDashboard_bienHasNoLocataire_throwsForbidden() {

        bien.setLocataire(null);

        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienRepository.findById(10L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> dashboardService.getDashboard(auth, 10L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("getDashboard() — utilisateur différent du locataire → KupangaBusinessException 403")
    void getDashboard_notLocataire_throwsForbidden() {

        User autreUser = User.builder().id(99L).mail("autre@test.com").build();

        when(auth.getName()).thenReturn(autreUser.getMail());
        when(userService.getUserByEmail(autreUser.getMail())).thenReturn(autreUser);
        when(bienRepository.findById(10L)).thenReturn(Optional.of(bien));

        KupangaBusinessException ex = assertThrows(KupangaBusinessException.class,
                () -> dashboardService.getDashboard(auth, 10L));

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(contratRepository, quittanceRepository, etatDesLieuxRepository);
    }

    // ══════════════════════════════════════════════════════════════
    // Fixture
    // ══════════════════════════════════════════════════════════════

    private void stubDependencies(
            List<Contrat> contrats,
            List<Quittance> quittances,
            List<EtatDesLieux> edls) {

        when(userService.getUserByEmail(locataire.getMail())).thenReturn(locataire);
        when(bienRepository.findById(10L)).thenReturn(Optional.of(bien));
        when(contratRepository.findByBienId(10L)).thenReturn(contrats);
        when(quittanceRepository.findByBienIdAndLocataireId(10L, locataire.getId()))
                .thenReturn(quittances);

        when(etatDesLieuxRepository.findByBienId(10L)).thenReturn(edls);

        // Pour chaque EDL qui passe le filtre locataire, on stub findWithAllRelations
        edls.stream()
                .filter(e -> e.getLocataire() != null
                        && e.getLocataire().getId().equals(locataire.getId()))
                .forEach(e -> when(etatDesLieuxRepository.findWithAllRelations(e.getId()))
                        .thenReturn(Optional.of(e)));
    }
}
