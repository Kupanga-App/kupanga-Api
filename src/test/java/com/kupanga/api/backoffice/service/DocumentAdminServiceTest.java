package com.kupanga.api.backoffice.service;

import com.kupanga.api.backoffice.dto.BienDocumentsSummaryDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.repository.ContratRepository;
import com.kupanga.api.immobilier.repository.EtatDesLieuxRepository;
import com.kupanga.api.immobilier.repository.QuittanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — DocumentAdminService")
class DocumentAdminServiceTest {

    @Mock private ContratRepository      contratRepository;
    @Mock private EtatDesLieuxRepository edlRepository;
    @Mock private QuittanceRepository    quittanceRepository;
    @Mock private BienRepository         bienRepository;

    @InjectMocks
    private DocumentAdminService documentAdminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("countTotalContrats() — délègue count au repository")
    void countTotalContrats_delegatesToRepository() {
        when(contratRepository.count()).thenReturn(10L);

        assertThat(documentAdminService.countTotalContrats()).isEqualTo(10L);
    }

    @Test
    @DisplayName("countTotalEdl() — délègue count au repository")
    void countTotalEdl_delegatesToRepository() {
        when(edlRepository.count()).thenReturn(5L);

        assertThat(documentAdminService.countTotalEdl()).isEqualTo(5L);
    }

    @Test
    @DisplayName("countTotalQuittances() — délègue count au repository")
    void countTotalQuittances_delegatesToRepository() {
        when(quittanceRepository.count()).thenReturn(20L);

        assertThat(documentAdminService.countTotalQuittances()).isEqualTo(20L);
    }

    @Test
    @DisplayName("getDocumentsParBien() — agrège contrats, EDL et quittances par bien, filtre total=0")
    void getDocumentsParBien_aggregatesAndFiltersEmpty() {
        Bien b1 = Bien.builder().id(1L).titre("Appart Paris").ville("Paris").build();
        Bien b2 = Bien.builder().id(2L).titre("Maison Lyon").ville("Lyon").build();

        when(bienRepository.findAll()).thenReturn(List.of(b1, b2));
        when(contratRepository.countParBien()).thenReturn(Collections.singletonList(new Object[]{1L, 3L}));
        when(edlRepository.countParBien()).thenReturn(Collections.singletonList(new Object[]{1L, 2L}));
        when(quittanceRepository.countParBien()).thenReturn(Collections.emptyList());

        List<BienDocumentsSummaryDTO> result = documentAdminService.getDocumentsParBien();

        // b1 a 3 contrats + 2 EDL → inclus ; b2 a 0 → exclu
        assertThat(result).hasSize(1);
        assertThat(result.get(0).bienId()).isEqualTo(1L);
        assertThat(result.get(0).nbContrats()).isEqualTo(3L);
        assertThat(result.get(0).nbEdl()).isEqualTo(2L);
        assertThat(result.get(0).total()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getDocumentsParBien() — trie par total décroissant")
    void getDocumentsParBien_trieParTotalDecroissant() {
        Bien b1 = Bien.builder().id(1L).titre("B1").ville("Paris").build();
        Bien b2 = Bien.builder().id(2L).titre("B2").ville("Lyon").build();

        when(bienRepository.findAll()).thenReturn(List.of(b1, b2));
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[]{1L, 1L});
        rows.add(new Object[]{2L, 5L});
        when(contratRepository.countParBien()).thenReturn(rows);
        when(edlRepository.countParBien()).thenReturn(Collections.emptyList());
        when(quittanceRepository.countParBien()).thenReturn(Collections.emptyList());

        List<BienDocumentsSummaryDTO> result = documentAdminService.getDocumentsParBien();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).total()).isGreaterThanOrEqualTo(result.get(1).total());
    }

    @Test
    @DisplayName("getContratsParBien() — délègue findByBienId et mappe en DTO")
    void getContratsParBien_delegatesAndMaps() {
        Contrat contrat = mock(Contrat.class);
        when(contratRepository.findByBienId(1L)).thenReturn(List.of(contrat));
        when(contrat.getId()).thenReturn(99L);

        var result = documentAdminService.getContratsParBien(1L);

        assertThat(result).hasSize(1);
        verify(contratRepository).findByBienId(1L);
    }

    @Test
    @DisplayName("getEdlParBien() — délègue findByBienId et mappe en DTO")
    void getEdlParBien_delegatesAndMaps() {
        EtatDesLieux edl = mock(EtatDesLieux.class);
        when(edlRepository.findByBienId(1L)).thenReturn(List.of(edl));
        when(edl.getId()).thenReturn(88L);

        var result = documentAdminService.getEdlParBien(1L);

        assertThat(result).hasSize(1);
        verify(edlRepository).findByBienId(1L);
    }

    @Test
    @DisplayName("getQuittancesParBien() — délègue findByBienId et mappe en DTO")
    void getQuittancesParBien_delegatesAndMaps() {
        Quittance quittance = mock(Quittance.class);
        when(quittanceRepository.findByBienId(1L)).thenReturn(List.of(quittance));
        when(quittance.getId()).thenReturn(77L);

        var result = documentAdminService.getQuittancesParBien(1L);

        assertThat(result).hasSize(1);
        verify(quittanceRepository).findByBienId(1L);
    }
}
