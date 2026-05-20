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
    public record Attachment(String url, String name) {}
}
