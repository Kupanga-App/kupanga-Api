package com.kupanga.api.immobilier.pdf;

import com.kupanga.api.immobilier.entity.EtatDesLieux;
import com.kupanga.api.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

import static com.kupanga.api.minio.constant.MinioConstant.EDL_BUCKET;

@Service
@RequiredArgsConstructor
@Slf4j
public class EtatDesLieuxPdfService {

    private final TemplateEngine templateEngine;
    private final MinioService   minioService;

    /**
     * Génère le PDF de l'état des lieux depuis le template Thymeleaf
     * et l'uploade sur MinIO.
     *
     * @param edl l'entité EtatDesLieux complète (avec bien, pièces, éléments, compteurs, clés)
     * @return l'URL MinIO du PDF généré
     */
    public String genererEtUploaderPdf(EtatDesLieux edl) {
        try {
            // 1 — Alimenter le contexte Thymeleaf
            Context ctx = new Context();
            ctx.setVariable("edl",          edl);
            ctx.setVariable("proprietaire", edl.getProprietaire());
            ctx.setVariable("locataire",    edl.getLocataire());

            // 2 — Rendu HTML via Thymeleaf
            String html = templateEngine.process("etat-des-lieux", ctx);

            // 3 — Conversion HTML → PDF via Flying Saucer
            byte[] pdfBytes = htmlToPdf(html);

            // 4 — Nom du fichier PDF
            String fileName = buildFileName(edl);

            // 5 — Upload sur MinIO et retour de l'URL
            return minioService.uploadPdf(pdfBytes, fileName, EDL_BUCKET);

        } catch (Exception e) {
            log.error("Erreur génération PDF EDL {} : {}", edl.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur génération PDF état des lieux", e);
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
     * Construit un nom de fichier lisible et unique.
     * Exemple : edl_entree_42_2026-03-19.pdf
     */
    private String buildFileName(EtatDesLieux edl) {
        String type = edl.getType().name().toLowerCase();   // "entree" ou "sortie"
        String date = edl.getDateRealisation().toString();  // "2026-03-19"
        return String.format("edl_%s_%d_%s.pdf", type, edl.getId(), date);
    }
}