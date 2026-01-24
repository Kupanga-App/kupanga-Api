package com.kupanga.api.utilisateur.controller;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.utilisateur.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.utilisateur.entity.Role;
import com.kupanga.api.utilisateur.service.AvatarProfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    // =========================================
    // CREATION AVATAR PROFIL
    // =========================================
    @Operation(
            summary = "Créer des avatars de profil",
            description = "Permet à un administrateur de téléverser un ou plusieurs avatars de profil. " +
                    "Les images sont stockées et les URLs générées sont retournées sous forme de DTO."
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Avatars de profil créés avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvatarProfilDTO.class),
                            examples = @ExampleObject(value = """
                                [
                                    {
                                        "id": 1,
                                        "url": "https://storage.kupanga.com/avatars/avatar1.png"
                                    },
                                    {
                                        "id": 2,
                                        "url": "https://storage.kupanga.com/avatars/avatar2.png"
                                    }
                                ]
                                """)
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé : l'utilisateur n'a pas le rôle ADMIN",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "l'utilisateur n'a pas les droits suffisants pour accéder à cette ressource"
                                }
                                """)
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : fichiers manquants ou incorrects",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Aucun fichier valide fourni"
                                }
                                """)
                    )
            )
    })
    @PostMapping("/avatars")
    public ResponseEntity<List<AvatarProfilDTO>> createAvatarProfil(
            @RequestParam("images") List<MultipartFile> images
    ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().iterator().next().getAuthority();

        if (!role.equalsIgnoreCase(Role.ROLE_ADMIN.name())) {
            throw new InvalidRoleException(
                    "l'utilisateur n'a pas les droits suffisants pour accéder à cette ressource. Rôle actuel de l'utilisateur : " + role
            );
        }

        return ResponseEntity.ok(avatarProfilService.createAvatarsProfil(images));
    }

}
