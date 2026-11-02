package com.kupanga.api.stockage.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = StockageConfig.class,
        properties = {
                "stockage.endpoint=http://stockage-interne:9000",
                "stockage.url-publique=https://fichiers.kupanga.test/",
                "stockage.region=eu-west-3",
                "stockage.path-style=true",
                "stockage.access-key=cle-service",
                "stockage.secret-key=secret-service",
                "stockage.gerer-politiques=false"
        }
)
@DisplayName("StockageConfig : clients S3 configurés par stockage.*")
class StockageConfigTest {

    @Autowired
    private S3Client s3Client;

    @Autowired
    @Qualifier("s3ClientAnonyme")
    private S3Client s3ClientAnonyme;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private StockageProperties proprietes;

    @Test
    @DisplayName("Propriétés lues, slash final de l'URL publique retiré")
    void proprietes() {
        assertThat(proprietes.urlPublique()).isEqualTo("https://fichiers.kupanga.test");
        assertThat(proprietes.gererPolitiques()).isFalse();
        assertThat(s3Client).isNotSameAs(s3ClientAnonyme);
        assertThat(proprietes.toString()).doesNotContain("secret-service").doesNotContain("cle-service");
    }

    @Test
    @DisplayName("P0-7 : URL présignée sur l'hôte public, en path-style, avec la région configurée, sans appel réseau")
    void urlPresigneeSurHotePublic() {
        String url = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(g -> g.bucket("contrat-de-bail").key("cle.pdf"))
                        .build())
                .url().toString();

        assertThat(url).startsWith("https://fichiers.kupanga.test/contrat-de-bail/cle.pdf?")
                .contains("X-Amz-Expires=300")
                .contains("eu-west-3")
                .contains("X-Amz-Signature=")
                .doesNotContain("stockage-interne")
                .doesNotContain("secret-service");
    }
}
