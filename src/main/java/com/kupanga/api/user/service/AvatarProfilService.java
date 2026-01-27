package com.kupanga.api.user.service;

import com.kupanga.api.user.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.user.entity.AvatarProfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AvatarProfilService {

    /**
     * Créer les avatars de profil et le stocke dans la BD
     * @param images les avatars
     * @return Avatar de profil
     */
    List<AvatarProfilDTO> createAvatarsProfil(List<MultipartFile> images);

    Page<AvatarProfil> finAll(Specification<AvatarProfil> avatarProfilSpecification , Pageable pageable);
}
