package com.kupanga.api.immobilier.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kupanga.api.immobilier.entity.PoiType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests unitaires — PoiSearchService")
@SuppressWarnings("unchecked")
class PoiSearchServiceTest {

    @Mock private WebClient                        webClient;
    @Mock private WebClient.RequestHeadersUriSpec  requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec     requestHeadersSpec;
    @Mock private WebClient.ResponseSpec           responseSpec;

    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PoiSearchService poiSearchService;

    private Point point;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        point = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(2.3522, 48.8566));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("calculerTousLesPoi() — réponse avec 2 éléments → map complète, count=2 par type")
    void calculerTousLesPoi_validResponse_returnsFilledMap() {
        String json = "{\"elements\":[{\"id\":1},{\"id\":2}]}";
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        Map<PoiType, Integer> result = poiSearchService.calculerTousLesPoi(point);

        assertThat(result).hasSize(PoiType.values().length);
        assertThat(result.values()).allMatch(count -> count == 2);
    }

    @Test
    @DisplayName("calculerTousLesPoi() — tableau elements vide → count=0 pour chaque type")
    void calculerTousLesPoi_emptyElements_returnsZeroForAllTypes() {
        String json = "{\"elements\":[]}";
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(json));

        Map<PoiType, Integer> result = poiSearchService.calculerTousLesPoi(point);

        assertThat(result).hasSize(PoiType.values().length);
        assertThat(result.values()).allMatch(count -> count == 0);
    }

    @Test
    @DisplayName("calculerTousLesPoi() — WebClient échoue → count=0 (exception avalée)")
    void calculerTousLesPoi_webClientError_returnsZeroForAllTypes() {
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Overpass indisponible")));

        Map<PoiType, Integer> result = poiSearchService.calculerTousLesPoi(point);

        assertThat(result).hasSize(PoiType.values().length);
        assertThat(result.values()).allMatch(count -> count == 0);
    }
}
