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
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public void createBien(Authentication auth , BienFormDTO bienFormDTO , List<MultipartFile> files){

        User user = userService.getUserByEmail(auth.getName());
        Role role = user.getRole();
        userService.verifyIfUserIsOwner(role);
        if(files.isEmpty()){
            throw new KupangaBusinessException("Les photos pour le bien publié sont obligatoires sur nôtre site"
                    , HttpStatus.BAD_REQUEST);
        }

        Bien bien = Bien.builder()
                .titre(bienFormDTO.getTitre())
                .typeBien(bienFormDTO.getTypeBien())
                .description(bienFormDTO.getDescription())
                .adresse(bienFormDTO.getAdresse())
                .ville(bienFormDTO.getVille())
                .codePostal(bienFormDTO.getCodePostal())
                .pays(bienFormDTO.getPays())
                .proprietaire(user)
                .build();

        geocodingService.geocode(bien.getAdresse() , bien.getVille() , bien.getCodePostal() , bien.getPays())
                        .ifPresentOrElse(
                                point -> {
                                    bien.setLocalisation(point);
                                    log.info("Géocodage réussi ->{} ", point);
                                },
                                () -> {
                                    throw new KupangaBusinessException("Nous n'avons pas pu géolocalisé votre bien "
                                            , HttpStatus.NOT_FOUND
                                    );
                                }
                        );
        bienRepository.save(bien);

        bienPoiService.calculerEtSauvegarderPoi(bien);

        bienImageService.uploadImagesImo(files , PHOTO_IMO_BUCKET , bien);

    }

    @Override
    public BienDTO getBienInfos(Long id){

        Bien bien = bienRepository.findWithAllProperties(id)
                .orElseThrow(
                        () -> new KupangaBusinessException("Le bien n'existe pas" , HttpStatus.NOT_FOUND)
                );

        return bienMapper.toDTO(bien);

    }

    @Override
    public Bien findWithAllProperties(Long id){

        return bienRepository.findWithAllProperties(id)
                .orElseThrow(
                        () -> new KupangaBusinessException("Le bien n'existe pas" , HttpStatus.NOT_FOUND)
                );
    }
}
