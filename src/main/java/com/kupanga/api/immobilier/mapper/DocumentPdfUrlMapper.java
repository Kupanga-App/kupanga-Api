package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import static com.kupanga.api.minio.constant.MinioConstant.CONTRAT_BUCKET;
import static com.kupanga.api.minio.constant.MinioConstant.EDL_BUCKET;
import static com.kupanga.api.minio.constant.MinioConstant.QUITTANCE_BUCKET;

/**
 * Transforme la clé d'un PDF stocké dans un bucket privé en URL présignée de courte durée (P0-7).
 * Utilisé par les mappers MapStruct et par la vue privée des biens : les DTO gardent un champ
 * {@code urlPdf}, mais il expire au bout de quelques minutes.
 */
@Component
@RequiredArgsConstructor
public class DocumentPdfUrlMapper {

    private final MinioService minioService;

    @Named("urlContrat")
    public String urlContrat(String clePdf) {
        return minioService.urlPresignee(CONTRAT_BUCKET, clePdf);
    }

    @Named("urlEtatDesLieux")
    public String urlEtatDesLieux(String clePdf) {
        return minioService.urlPresignee(EDL_BUCKET, clePdf);
    }

    @Named("urlQuittance")
    public String urlQuittance(String clePdf) {
        return minioService.urlPresignee(QUITTANCE_BUCKET, clePdf);
    }
}
