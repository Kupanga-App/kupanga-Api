package com.kupanga.api.utilisateur.controller;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.service.AvatarProfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AvatarProfilController {

    private final AvatarProfilService avatarProfilService;

    @PostMapping("/avatars")
    public ResponseEntity<List<AvatarProfilDTO>> createAvatarProfil(@RequestParam("images")List<MultipartFile> images){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        if(!role.equalsIgnoreCase(String.valueOf(Role.ROLE_ADMIN))){

            throw new InvalidRoleException("l'utilisateur n'a pas les droits suffisants pour accéder à cette ressource. Rôle actuel de l'utilisateur : " + role);
        }

        return ResponseEntity.ok(avatarProfilService.createAvatarsProfil(images));
    }
}
