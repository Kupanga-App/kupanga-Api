package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.readDTO.LocataireDashboardDTO;
import org.springframework.security.core.Authentication;

public interface LocataireDashboardService {

    /**
     * Retourne la vue agrégée du bail pour un locataire authentifié.
     * L'utilisateur connecté doit être le locataire du bien demandé.
     *
     * @throws com.kupanga.api.exception.business.KupangaBusinessException 404 si le bien n'existe pas
     * @throws com.kupanga.api.exception.business.KupangaBusinessException 403 si l'utilisateur n'est pas locataire de ce bien
     */
    LocataireDashboardDTO getDashboard(Authentication auth, Long bienId);
}
