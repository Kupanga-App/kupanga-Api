package com.kupanga.api.login.controller;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.service.CreationCompteService;
import com.kupanga.api.utilisateur.dto.formDTO.UtilisateurFormDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UtilisateurDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/creationCompte")
public class CreationCompteController {

    private final CreationCompteService creationCompteService;

    /**
     * Endpoint pour créer un nouvel utilisateur.
     *
     * <p>Cette méthode permet de créer un compte utilisateur avec l'email et le rôle
     * fournis dans le corps de la requête. Elle vérifie si l'utilisateur existe déjà
     * et lance une exception si c'est le cas.</p>
     *
     * <p>Exemples de cas :
     * <ul>
     *     <li>Email déjà existant -> {@link UserAlreadyExistsException}</li>
     *     <li>Rôle invalide -> {@link com.kupanga.api.exception.business.InvalidRoleException}</li>
     *     <li>Création réussie → renvoie l'objet {@link UtilisateurDTO} correspondant</li>
     * </ul>
     * </p>
     *
     * @param utilisateurFormDTO DTO contenant l'email et le rôle de l'utilisateur à créer
     * @return ResponseEntity contenant l'objet {@link UtilisateurDTO} créé
     * @throws UserAlreadyExistsException si un utilisateur avec le même email existe déjà
     */

    @Operation(
            summary = "Créer un nouvel utilisateur",
            description = "Crée un utilisateur avec un email et un rôle. Retourne le DTO de l'utilisateur créé."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur créé avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UtilisateurDTO.class))),
            @ApiResponse(responseCode = "409", description = "Un utilisateur avec cet email existe déjà",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Rôle invalide fourni",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<UtilisateurDTO> creationCompte(
            @RequestBody @Parameter(description = "Données du nouvel utilisateur", required = true) UtilisateurFormDTO utilisateurFormDTO
    ) throws UserAlreadyExistsException {

        return ResponseEntity.ok(creationCompteService
                .creationUtilisateur(utilisateurFormDTO.email(), utilisateurFormDTO.role()));
    }

}
