package com.kupanga.api.backoffice.dto;

public record BienDocumentsSummaryDTO(
        Long   bienId,
        String titre,
        String ville,
        long   nbContrats,
        long   nbEdl,
        long   nbQuittances
) {
    public long total() {
        return nbContrats + nbEdl + nbQuittances;
    }
}
