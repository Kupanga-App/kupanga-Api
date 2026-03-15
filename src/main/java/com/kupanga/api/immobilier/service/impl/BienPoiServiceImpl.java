package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienPoi;
import com.kupanga.api.immobilier.entity.PoiType;
import com.kupanga.api.immobilier.repository.BienPoiRepository;
import com.kupanga.api.immobilier.research.PoiSearchService;
import com.kupanga.api.immobilier.service.BienPoiService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BienPoiServiceImpl implements BienPoiService {

    private final PoiSearchService poiSearchService;
    private final BienPoiRepository bienPoiRepository;

    @Override
    @Async
    @Transactional
    public void calculerEtSauvegarderPoi(Bien bien) {
        if (bien.getLocalisation() == null) {
            log.warn("Bien {} sans localisation — calcul POI ignoré", bien.getId());
            return;
        }

        log.info("Calcul POI async pour le bien {}", bien.getId());

        Map<PoiType, Integer> resultats =
                poiSearchService.calculerTousLesPoi(bien.getLocalisation());

        List<BienPoi> bienPois = resultats.entrySet().stream()
                .map(entry -> BienPoi.builder()
                        .bien(bien)
                        .poiType(entry.getKey())
                        .present(entry.getValue() > 0)
                        .nombreTrouve(entry.getValue())
                        .rayonMetres(500.0)
                        .build())
                .toList();

        bienPoiRepository.saveAll(bienPois);
        log.info("POI sauvegardés pour le bien {}", bien.getId());
    }
}
