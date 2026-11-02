package com.kupanga.api.user.mapper;

import com.kupanga.api.stockage.service.StockageService;
import com.kupanga.api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import static com.kupanga.api.stockage.constant.StockageConstant.DUREE_URL_PHOTO_PROFIL;
import static com.kupanga.api.stockage.constant.StockageConstant.PHOTO_PROFIL_BUCKET;

/**
 * S2 : URL de la photo de profil d'un compte. Notre photo est dans un bucket <b>privé</b> : URL présignée de 24 h
 * (décision du 2026-11-02) ; sinon photo externe du compte Google, ou rien.
 */
@Component
@RequiredArgsConstructor
public class PhotoProfilUrlMapper {

    private final StockageService stockageService;

    @Named("urlPhotoProfil")
    public String urlPhotoProfil(User user) {
        if (user == null) return null;
        if (user.getClePhotoProfil() != null) {
            return stockageService.urlPresignee(PHOTO_PROFIL_BUCKET, user.getClePhotoProfil(), DUREE_URL_PHOTO_PROFIL);
        }
        return user.getUrlProfile();
    }
}
