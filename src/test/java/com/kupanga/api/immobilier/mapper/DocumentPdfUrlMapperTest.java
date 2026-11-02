package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.stockage.service.StockageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.kupanga.api.stockage.constant.StockageConstant.CONTRAT_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.EDL_BUCKET;
import static com.kupanga.api.stockage.constant.StockageConstant.QUITTANCE_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentPdfUrlMapperTest {

    private final StockageService stockageService = mock(StockageService.class);
    private final DocumentPdfUrlMapper mapper = new DocumentPdfUrlMapper(stockageService);

    @Test
    @DisplayName("Chaque type de document est signé dans son propre bucket privé (P0-7)")
    void urlPresignee_utiliseLeBonBucket() {
        when(stockageService.urlPresignee(CONTRAT_BUCKET, "c.pdf")).thenReturn("signe-contrat");
        when(stockageService.urlPresignee(EDL_BUCKET, "e.pdf")).thenReturn("signe-edl");
        when(stockageService.urlPresignee(QUITTANCE_BUCKET, "q.pdf")).thenReturn("signe-quittance");

        assertThat(mapper.urlContrat("c.pdf")).isEqualTo("signe-contrat");
        assertThat(mapper.urlEtatDesLieux("e.pdf")).isEqualTo("signe-edl");
        assertThat(mapper.urlQuittance("q.pdf")).isEqualTo("signe-quittance");
    }
}
