package com.kupanga.api.backoffice.dto;

import com.kupanga.api.email.file.StatutEmail;
import com.kupanga.api.email.file.TypeEmail;

/** M4 : filtres de la page « E-mails » ; pagination ramenée dans les bornes du back-office. */
public record EmailAdminSearchDTO(
        StatutEmail statut,
        TypeEmail   type,
        int         page,
        int         size
) {
    public static final int SIZE_MAX = 100;

    public EmailAdminSearchDTO {
        page = Math.min(Math.max(0, page), Integer.MAX_VALUE / SIZE_MAX); // décalage SQL sans débordement
        size = Math.min(Math.max(1, size), SIZE_MAX);
    }
}
