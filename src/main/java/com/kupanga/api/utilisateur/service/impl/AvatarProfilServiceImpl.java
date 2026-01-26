package com.kupanga.api.utilisateur.service.impl;

import com.kupanga.api.minio.service.MinioService;
import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.utilisateur.entity.AvatarProfil;
import com.kupanga.api.utilisateur.mapper.AvatarProfilMapper;
import com.kupanga.api.utilisateur.repository.AvatarProfilRepository;
import com.kupanga.api.utilisateur.service.AvatarProfilService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.kupanga.api.minio.constant.MinioConstant.AVATAR_PROFIL_BUCKET;

@Service
@RequiredArgsConstructor
public class AvatarProfilServiceImpl implements AvatarProfilService {

    private final AvatarProfilRepository avatarProfilRepository;
    private final MinioService minioService ;
    private final AvatarProfilMapper avatarProfilMapper;

    @Transactional
    @Override
    public List<AvatarProfilDTO> createAvatarsProfil(List<MultipartFile> images) {

        List<AvatarProfilDTO> avatarsDTO = new ArrayList<>();

        for (MultipartFile image : images) {
            // Upload du fichier vers MinIO
            String imageUrl = minioService.uploadImage(image, AVATAR_PROFIL_BUCKET);

            // Création de l'entité
            AvatarProfil avatarProfil = new AvatarProfil();
            avatarProfil.setUrl(imageUrl);
            avatarProfilRepository.save(avatarProfil);

            // Conversion en DTO
            avatarsDTO.add(avatarProfilMapper.toDTO(avatarProfil));
        }

        return avatarsDTO;
    }

    @Override
    public Page<AvatarProfil> finAll(Specification<AvatarProfil> avatarProfilSpecification , Pageable pageable){

        return avatarProfilRepository.findAll(avatarProfilSpecification , pageable);
    }


}
