package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.entity.BienImage;
import com.kupanga.api.stockage.service.StockageService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.kupanga.api.stockage.constant.StockageConstant.PHOTO_IMO_BUCKET;

/**
 * S2 : construit l'URL publique des photos d'un bien depuis leur clé (bucket public des photos de biens).
 * Une photo antérieure à V51 dont la clé n'a pas pu être extraite garde son ancienne URL.
 */
@Component
@RequiredArgsConstructor
public class PhotoBienUrlMapper {

    private final StockageService stockageService;

    /** Photos (1600 px) dans l'ordre d'envoi (id) : la première sert de couverture. */
    @Named("imageUrls")
    public List<String> images(Set<BienImage> images) {
        return photosTriees(images).stream().map(this::photo).filter(Objects::nonNull).toList();
    }

    /** R1 : vignette de chaque photo (la photo elle-même si elle n'en a pas), dans l'ordre de {@link #images}. */
    @Named("vignetteUrls")
    public List<String> vignettes(Set<BienImage> images) {
        return photosTriees(images).stream().map(this::vignette).filter(Objects::nonNull).toList();
    }

    public String photo(BienImage image) {
        return image.getCle() != null ? stockageService.urlPublique(PHOTO_IMO_BUCKET, image.getCle()) : image.getUrl();
    }

    public String vignette(BienImage image) {
        if (image.getCleVignette() != null) {
            return stockageService.urlPublique(PHOTO_IMO_BUCKET, image.getCleVignette());
        }
        return image.getUrlVignette() != null ? image.getUrlVignette() : photo(image);
    }

    /** Ordre d'envoi (id) : stable entre {@code images} et {@code vignettes}. */
    static List<BienImage> photosTriees(Set<BienImage> images) {
        if (images == null) return List.of();
        return images.stream()
                .filter(image -> image.getCle() != null || image.getUrl() != null)
                .sorted(Comparator.comparing(BienImage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
