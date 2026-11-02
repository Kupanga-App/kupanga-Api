package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.stockage.service.StockageService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import static com.kupanga.api.stockage.constant.StockageConstant.CONTRAT_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.EDL_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.QUITTANCE_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.RECU_BUCKET;

/**
 * Transforme la clé d'un PDF stocké dans un bucket privé en URL présignée de courte durée (P0-7).
 * Utilisé par les mappers MapStruct et par la vue privée des biens : les DTO gardent un champ
 * {@code urlPdf}, mais il expire au bout de quelques minutes.
 */
@Component
@RequiredArgsConstructor
public class DocumentPdfUrlMapper {

    private final StockageService stockageService;

    @Named("urlContrat")
    public String urlContrat(String clePdf) {
        return stockageService.urlPresignee(CONTRAT_BUCKET, clePdf);
    }

    @Named("urlEtatDesLieux")
    public String urlEtatDesLieux(String clePdf) {
        return stockageService.urlPresignee(EDL_BUCKET, clePdf);
    }

    @Named("urlQuittance")
    public String urlQuittance(String clePdf) {
        return stockageService.urlPresignee(QUITTANCE_BUCKET, clePdf);
    }

    /** C8-bis : photo du reçu de paiement (bucket privé). */
    @Named("urlRecu")
    public String urlRecu(String cleRecu) {
        return stockageService.urlPresignee(RECU_BUCKET, cleRecu);
    }
}
