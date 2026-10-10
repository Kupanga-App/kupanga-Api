package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.BienPoi;
import com.kupanga.api.immobilier.entity.PoiType;
import com.kupanga.api.immobilier.repository.BienPoiRepository;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.research.PoiSearchService;
import com.kupanga.api.immobilier.service.BienPoiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.kupanga.api.immobilier.constant.Constant.RAYON_DEFAUT;

@Service
@RequiredArgsConstructor
@Slf4j
public class BienPoiServiceImpl implements BienPoiService {

    private final PoiSearchService poiSearchService;
    private final BienPoiRepository bienPoiRepository;
    private final BienRepository bienRepository;

    /**
     * Sans transaction englobante : l'appel à Overpass (jusqu'à ~23 s) ne doit pas garder une connexion
     * à la base ; seuls la lecture du bien et {@code saveAll} ouvrent chacun leur transaction.
     */
    @Override
    @Async
    public void calculerEtSauvegarderPoi(Long bienId) {
        Bien bien = bienRepository.findById(bienId).orElse(null);
        if (bien == null || bien.getLocalisation() == null) {
            log.warn("Bien {} introuvable ou sans localisation — calcul POI ignoré", bienId);
            return;
        }

        log.info("Calcul POI async pour le bien {}", bienId);

        Map<PoiType, Integer> resultats = poiSearchService.calculerTousLesPoi(bien.getLocalisation());
        if (resultats.isEmpty()) {
            // Overpass indisponible : rien n'est enregistré plutôt que « aucun POI »
            log.warn("POI non calculés pour le bien {} (Overpass indisponible)", bienId);
            return;
        }

        List<BienPoi> bienPois = resultats.entrySet().stream()
                .map(entry -> BienPoi.builder()
                        .bien(bien)
                        .poiType(entry.getKey())
                        .present(entry.getValue() > 0)
                        .nombreTrouve(entry.getValue())
                        .rayonMetres(RAYON_DEFAUT) // rayon réellement interrogé
                        .build())
                .toList();

        bienPoiRepository.saveAll(bienPois);
        log.info("POI sauvegardés pour le bien {}", bienId);
    }
}
