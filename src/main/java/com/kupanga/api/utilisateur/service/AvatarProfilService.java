package com.kupanga.api.utilisateur.service;

import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AvatarProfilService {

    /**
     * Créer les avatars de profil et le stocke dans la BD
     * @param images les avatars
     * @return Avatar de profil
     */
    List<AvatarProfilDTO> createAvatarsProfil(List<MultipartFile> images);
}
