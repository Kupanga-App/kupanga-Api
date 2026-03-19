package com.kupanga.api.immobilier.pdf;

import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static com.kupanga.api.minio.constant.MinioConstant.QUITTANCE_BUCKET;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuittancePdfService {

    private final TemplateEngine templateEngine;
    private final MinioService   minioService;

    // Mapping mois → libellé français
    public static final Map<Integer, String> MOIS_LABELS = Map.ofEntries(
            Map.entry(1,  "Janvier"),
            Map.entry(2,  "Février"),
            Map.entry(3,  "Mars"),
            Map.entry(4,  "Avril"),
            Map.entry(5,  "Mai"),
            Map.entry(6,  "Juin"),
            Map.entry(7,  "Juillet"),
            Map.entry(8,  "Août"),
            Map.entry(9,  "Septembre"),
            Map.entry(10, "Octobre"),
            Map.entry(11, "Novembre"),
            Map.entry(12, "Décembre")
    );

    /**
     * Génère le PDF de la quittance depuis le template Thymeleaf
     * et l'uploade sur MinIO.
     *
     * @param quittance l'entité Quittance complète (avec bien, propriétaire, locataire)
     * @return l'URL MinIO du PDF généré
     */
    public String genererEtUploaderPdf(Quittance quittance) {
        try {
            // 1 — Alimenter le contexte Thymeleaf
            Context ctx = new Context();
            ctx.setVariable("quittance",    quittance);
            ctx.setVariable("proprietaire", quittance.getProprietaire());
            ctx.setVariable("locataire",    quittance.getLocataire());
            ctx.setVariable("moisLabel",    MOIS_LABELS.getOrDefault(quittance.getMois(), "—"));

            // 2 — Rendu HTML via Thymeleaf
            String html = templateEngine.process("quittance", ctx);

            // 3 — Conversion HTML → PDF via Flying Saucer
            byte[] pdfBytes = htmlToPdf(html);

            // 4 — Nom du fichier
            String fileName = buildFileName(quittance);

            // 5 — Upload MinIO
            return minioService.uploadPdf(pdfBytes, fileName, QUITTANCE_BUCKET);

        } catch (Exception e) {
            log.error("Erreur génération PDF quittance {} : {}", quittance.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur génération PDF quittance", e);
        }
    }

    // ─── Helpers privés ───────────────────────────────────────────────────────

    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        }
    }

    /**
     * Exemple : quittance_42_2026_03.pdf
     */
    private String buildFileName(Quittance quittance) {
        return String.format("quittance_%d_%d_%02d.pdf",
                quittance.getId(),
                quittance.getAnnee(),
                quittance.getMois());
    }
}