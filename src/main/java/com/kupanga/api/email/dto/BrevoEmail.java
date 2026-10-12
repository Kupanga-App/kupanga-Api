package com.kupanga.api.email.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrevoEmail(
        Sender sender,
        List<Recipient> to,
        String subject,
        String htmlContent,
        List<Attachment> attachment
) {
    public record Sender(String name, String email) {}
    public record Recipient(String email) {}
    /**
     * Pièce jointe envoyée en base64 ({@code content}) : les PDF sont dans des buckets privés (P0-7),
     * Brevo ne peut donc plus les récupérer par URL.
     */
    public record Attachment(String content, String name) {}
}
