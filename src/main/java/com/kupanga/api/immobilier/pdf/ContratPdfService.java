package com.kupanga.api.immobilier.pdf;

import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

import static com.kupanga.api.minio.constant.MinioConstant.CONTRAT_BUCKET;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContratPdfService {

    private final TemplateEngine templateEngine;
    private final MinioService minioService;

    /**
     * Génère le PDF depuis le template Thymeleaf et l'upload sur MinIO.
     * Retourne l'URL MinIO du PDF généré.
     */
    public String genererEtUploaderPdf(Contrat contrat) {
        try {
            // 1 — Alimenter le contexte Thymeleaf
            Context ctx = new Context();
            ctx.setVariable("contrat",      contrat);
            ctx.setVariable("proprietaire", contrat.getProprietaire());
            ctx.setVariable("locataire",    contrat.getLocataire());

            // 2 — Rendu HTML via Thymeleaf
            String html = templateEngine.process("contrat", ctx);

            // 3 — Conversion HTML → PDF via Flying Saucer
            byte[] pdfBytes = htmlToPdf(html);

            // 4 — nom du pdf
            String originalName = "contrats_numero_" + contrat.getId() + ".pdf";

            return minioService.uploadPdf(pdfBytes, originalName, CONTRAT_BUCKET);

        } catch (Exception e) {
            log.error("Erreur génération PDF contrat {} : {}", contrat.getId(), e.getMessage());
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    private byte[] htmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        }
    }
}
