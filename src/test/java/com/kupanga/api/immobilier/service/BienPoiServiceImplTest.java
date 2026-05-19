package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienPoi;
import com.kupanga.api.immobilier.entity.PoiType;
import com.kupanga.api.immobilier.repository.BienPoiRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — BienPoiServiceImpl")
class BienPoiServiceImplTest {

    @Mock private PoiSearchService  poiSearchService;
    @Mock private BienPoiRepository bienPoiRepository;

    @InjectMocks
    private BienPoiServiceImpl bienPoiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — localisation null → calcul ignoré, pas de saveAll")
    void calculerEtSauvegarderPoi_nullLocalisation_skipsSave() {
        Bien bien = Bien.builder().id(1L).localisation(null).build();

        bienPoiService.calculerEtSauvegarderPoi(bien);

        verify(poiSearchService, never()).calculerTousLesPoi(any());
        verify(bienPoiRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — localisation valide → 4 BienPoi construits et sauvegardés")
    @SuppressWarnings("unchecked")
    void calculerEtSauvegarderPoi_validLocalisation_savesFourPoi() {
        Point point = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(2.3522, 48.8566));
        Bien bien = Bien.builder().id(1L).localisation(point).build();

        Map<PoiType, Integer> poiMap = Map.of(
                PoiType.SCHOOL,       3,
                PoiType.HOSPITAL,     1,
                PoiType.PHARMACY,     2,
                PoiType.KINDERGARTEN, 0
        );
        when(poiSearchService.calculerTousLesPoi(point)).thenReturn(poiMap);

        bienPoiService.calculerEtSauvegarderPoi(bien);

        ArgumentCaptor<List<BienPoi>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienPoiRepository).saveAll(captor.capture());

        List<BienPoi> saved = captor.getValue();
        assertThat(saved).hasSize(4);
        assertThat(saved).extracting(BienPoi::getBien).containsOnly(bien);
        assertThat(saved).extracting(BienPoi::getRayonMetres).containsOnly(500.0);
    }

    @Test
    @DisplayName("calculerEtSauvegarderPoi() — 0 POI trouvé → BienPoi.present = false")
    @SuppressWarnings("unchecked")
    void calculerEtSauvegarderPoi_zeroPoi_presentIsFalse() {
        Point point = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(2.3522, 48.8566));
        Bien bien = Bien.builder().id(1L).localisation(point).build();

        Map<PoiType, Integer> poiMap = Map.of(
                PoiType.SCHOOL,       0,
                PoiType.HOSPITAL,     0,
                PoiType.PHARMACY,     0,
                PoiType.KINDERGARTEN, 0
        );
        when(poiSearchService.calculerTousLesPoi(point)).thenReturn(poiMap);

        bienPoiService.calculerEtSauvegarderPoi(bien);

        ArgumentCaptor<List<BienPoi>> captor = ArgumentCaptor.forClass(List.class);
        verify(bienPoiRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).extracting(BienPoi::getPresent).containsOnly(false);
    }
}
