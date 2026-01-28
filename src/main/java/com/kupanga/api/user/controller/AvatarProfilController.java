package com.kupanga.api.user.controller;

import com.kupanga.api.exception.business.InvalidRoleException;
import com.kupanga.api.user.dto.paginationDTO.AvatarProfilPagination;
import com.kupanga.api.user.dto.readDTO.AvatarProfilDTO;
import com.kupanga.api.user.research.researchDTO.AvatarProfileResearchDTO;
import com.kupanga.api.user.entity.Role;
import com.kupanga.api.user.service.AvatarProfilService;
import com.kupanga.api.user.research.impl.AvatarProfilResearchImpl;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/avatar")
@RequiredArgsConstructor
public class AvatarProfilController {

    private final AvatarProfilService avatarProfilService;
    private final AvatarProfilResearchImpl avatarProfilResearch;

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
    @PostMapping("/admin")
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

    // =========================================
    // RECHERCHE AVATAR PROFIL
    // =========================================
    @Operation(
            summary = "Rechercher des avatars de profil",
            description = "Permet à un utilisateur de rechercher des avatars de profil en fonction de critères " +
                    "tels que l'url, le type ou d'autres filtres définis dans le DTO de recherche. " +
                    "Renvoie une pagination des résultats correspondant aux critères."
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Résultats de recherche des avatars de profil retournés avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvatarProfilPagination.class),
                            examples = @ExampleObject(value = """
                            {
                                "page": 1,
                                "size": 10,
                                "totalElements": 25,
                                "totalPages": 3,
                                "content": [
                                    {
                                        "id": 1,
                                        "url": "https://storage.kupanga.com/avatars/avatar1.png",
                                    },
                                    {
                                        "id": 2,
                                        "url": "https://storage.kupanga.com/avatars/avatar2.png",
                                    }
                                ]
                            }
                            """)
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : les critères de recherche sont manquants ou incorrects",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "error": "Critères de recherche invalides ou manquants"
                            }
                            """)
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé : l'utilisateur n'a pas les droits suffisants",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "error": "Accès refusé : utilisateur non autorisé"
                            }
                            """)
                    )
            )
    })
    @PostMapping("/search")
    public ResponseEntity<AvatarProfilPagination> search(@RequestBody AvatarProfileResearchDTO avatarProfileResearchDTO) {
        return ResponseEntity.ok(avatarProfilResearch.research(avatarProfileResearchDTO));
    }
}
