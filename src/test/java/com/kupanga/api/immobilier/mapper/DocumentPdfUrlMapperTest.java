package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.minio.service.MinioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.kupanga.api.minio.constant.MinioConstant.CONTRAT_BUCKET;
import static com.kupanga.api.minio.constant.MinioConstant.EDL_BUCKET;
import static com.kupanga.api.minio.constant.MinioConstant.QUITTANCE_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentPdfUrlMapperTest {

    private final MinioService minioService = mock(MinioService.class);
    private final DocumentPdfUrlMapper mapper = new DocumentPdfUrlMapper(minioService);

    @Test
    @DisplayName("Chaque type de document est signé dans son propre bucket privé (P0-7)")
    void urlPresignee_utiliseLeBonBucket() {
        when(minioService.urlPresignee(CONTRAT_BUCKET, "c.pdf")).thenReturn("signe-contrat");
        when(minioService.urlPresignee(EDL_BUCKET, "e.pdf")).thenReturn("signe-edl");
        when(minioService.urlPresignee(QUITTANCE_BUCKET, "q.pdf")).thenReturn("signe-quittance");

        assertThat(mapper.urlContrat("c.pdf")).isEqualTo("signe-contrat");
        assertThat(mapper.urlEtatDesLieux("e.pdf")).isEqualTo("signe-edl");
        assertThat(mapper.urlQuittance("q.pdf")).isEqualTo("signe-quittance");
    }
}
