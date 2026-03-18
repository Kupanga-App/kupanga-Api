package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.exception.business.KupangaBusinessException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.mapper.BienMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.service.BienImageService;
import com.kupanga.api.immobilier.service.BienPoiService;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.immobilier.service.GeocodingService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.kupanga.api.minio.constant.MinioConstant.PHOTO_IMO_BUCKET;

@Service
@RequiredArgsConstructor
@Slf4j
public class BienServiceImpl implements BienService {

    private final UserService userService;
    private final BienImageService bienImageService;
    private final GeocodingService geocodingService;
    private final BienRepository bienRepository;
    private final BienMapper bienMapper;
    private final BienPoiService bienPoiService;

    public void createBien(Authentication auth, BienFormDTO dto, List<MultipartFile> files) {

        User user = userService.getUserByEmail(auth.getName());
        userService.verifyIfUserIsOwner(user.getRole());

        if (files == null || files.isEmpty()) {
            throw new KupangaBusinessException(
                    "Les photos pour le bien publié sont obligatoires sur notre site",
                    HttpStatus.BAD_REQUEST
            );
        }

        Bien bien = Bien.builder()
                // ─── Informations générales ───────────────────────────────────
                .titre(dto.getTitre())
                .typeBien(dto.getTypeBien())
                .description(dto.getDescription())
                .proprietaire(user)

                // ─── Adresse ──────────────────────────────────────────────────
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .codePostal(dto.getCodePostal())
                .pays(dto.getPays())

                // ─── Caractéristiques physiques ───────────────────────────────
                .surfaceHabitable(dto.getSurfaceHabitable())
                .nombrePieces(dto.getNombrePieces())
                .nombreChambres(dto.getNombreChambres())
                .etage(dto.getEtage())
                .ascenseur(dto.getAscenseur())
                .anneeConstruction(dto.getAnneeConstruction())
                .modeChauffage(dto.getModeChauffage())

                // ─── Diagnostic énergétique ───────────────────────────────────
                .classeEnergie(dto.getClasseEnergie())
                .classeGes(dto.getClasseGes())

                // ─── Conditions de location ───────────────────────────────────
                .loyerMensuel(dto.getLoyerMensuel())
                .chargesMensuelles(dto.getChargesMensuelles())
                .depotGarantie(dto.getDepotGarantie())
                .meuble(dto.getMeuble())
                .colocation(dto.getColocation())
                .disponibleDe(dto.getDisponibleDe())

                .build();

        Point point = geocodingService.geocode(dto.getAdresse(), dto.getVille(),
                dto.getCodePostal(), dto.getPays());

        if (point == null) {
            throw new KupangaBusinessException(
                    "Nous n'avons pas pu géolocaliser votre bien",
                    HttpStatus.NOT_FOUND
            );
        }

        bien.setLocalisation(point);
        log.info("Géocodage réussi -> {}", point);

        bienRepository.save(bien);

        bienPoiService.calculerEtSauvegarderPoi(bien);

        bienImageService.uploadImagesImo(files, PHOTO_IMO_BUCKET, bien);
    }

    @Override
    public BienDTO getBienInfos(Long id){

        Bien bien = bienRepository.findWithAllProperties(id)
                .orElseThrow(
                        () -> new KupangaBusinessException("Le bien n'existe pas" , HttpStatus.NOT_FOUND)
                );

        return bienMapper.toPublicDTO(bien);

    }

    @Override
    public Bien findWithAllProperties(Long id){

        return bienRepository.findWithAllProperties(id)
                .orElseThrow(
                        () -> new KupangaBusinessException("Le bien n'existe pas" , HttpStatus.NOT_FOUND)
                );
    }
}
