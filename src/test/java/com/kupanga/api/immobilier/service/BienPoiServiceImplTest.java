package com.kupanga.api.immobilier.service;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienPoi;
import com.kupanga.api.immobilier.entity.PoiType;
import com.kupanga.api.immobilier.repository.BienPoiRepository;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.research.PoiSearchService;
import com.kupanga.api.immobilier.service.impl.BienPoiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — BienPoiServiceImpl")
class BienPoiServiceImplTest {

    @Mock private PoiSearchService  poiSearchService;
    @Mock private BienPoiRepository bienPoiRepository;
    @Mock private BienRepository    bienRepository;

    @InjectMocks
    private BienPoiServiceImpl bienPoiService;

    private Point point;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        point = new GeometryFactory(new PrecisionModel(), 4326).createPoint(new Coordinate(2.3522, 48.8566));
    }

    private Bien bienEnBase(Point localisation) {
        Bien bien = Bien.builder().pays(Pays.FR).id(1L).localisation(localisation).build();
        when(bienRepository.findById(1L)).thenReturn(Optional.of(bien));
        return bien;
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — localisation null → calcul ignoré, pas de saveAll")
    void calculerEtSauvegarderPoi_nullLocalisation_skipsSave() {
        bienEnBase(null);

        bienPoiService.calculerEtSauvegarderPoi(1L);

        verify(poiSearchService, never()).calculerTousLesPoi(any());
        verify(bienPoiRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — bien introuvable (B9 : relu par id) → rien")
    void calculerEtSauvegarderPoi_bienIntrouvable_rien() {
        when(bienRepository.findById(1L)).thenReturn(Optional.empty());

        bienPoiService.calculerEtSauvegarderPoi(1L);

        verifyNoInteractions(poiSearchService);
        verify(bienPoiRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — Overpass indisponible (map vide) → rien d'enregistré, pas « aucun POI » (B9)")
    void calculerEtSauvegarderPoi_overpassIndisponible_rienEnregistre() {
        bienEnBase(point);
        when(poiSearchService.calculerTousLesPoi(point)).thenReturn(Map.of());

        bienPoiService.calculerEtSauvegarderPoi(1L);

        verify(bienPoiRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — localisation valide → 4 BienPoi rattachés au bien relu et sauvegardés")
    @SuppressWarnings("unchecked")
    void calculerEtSauvegarderPoi_validLocalisation_savesFourPoi() {
        Bien bien = bienEnBase(point);
        when(poiSearchService.calculerTousLesPoi(point)).thenReturn(Map.of(
                PoiType.SCHOOL,       3,
                PoiType.HOSPITAL,     1,
                PoiType.PHARMACY,     2,
                PoiType.KINDERGARTEN, 0
        ));

        bienPoiService.calculerEtSauvegarderPoi(1L);

        ArgumentCaptor<List<BienPoi>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienPoiRepository).saveAll(captor.capture());

        List<BienPoi> saved = captor.getValue();
        assertThat(saved).hasSize(4);
        assertThat(saved).extracting(BienPoi::getBien).containsOnly(bien);
        assertThat(saved).extracting(BienPoi::getRayonMetres).containsOnly(5000.0); // RAYON_DEFAUT, rayon interrogé
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — 0 POI trouvé → BienPoi.present = false")
    @SuppressWarnings("unchecked")
    void calculerEtSauvegarderPoi_zeroPoi_presentIsFalse() {
        bienEnBase(point);
        when(poiSearchService.calculerTousLesPoi(point)).thenReturn(Map.of(
                PoiType.SCHOOL,       0,
                PoiType.HOSPITAL,     0,
                PoiType.PHARMACY,     0,
                PoiType.KINDERGARTEN, 0
        ));

        bienPoiService.calculerEtSauvegarderPoi(1L);

        ArgumentCaptor<List<BienPoi>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienPoiRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).extracting(BienPoi::getPresent).containsOnly(false);
    }
}
