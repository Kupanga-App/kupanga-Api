package com.kupanga.api.utilisateur.controller;

import com.kupanga.api.exception.business.InvalidPasswordException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.utilisateur.service.UtilisateurService;
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
@RequestMapping("/utilisateur")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    /**
     * Endpoint pour connecter un utilisateur.
     *
     * <p>Cette méthode permet à un utilisateur existant de se connecter en fournissant son
     * email et son mot de passe dans le corps de la requête. Si les informations sont correctes,
     * elle renvoie un JWT d'accès, un refresh token, ainsi que le rôle et l'email de l'utilisateur.</p>
     *
     * <p>Cas possibles :
     * <ul>
     *     <li>Utilisateur inexistant -> {@link com.kupanga.api.exception.business.UserNotFoundException}</li>
     *     <li>Mot de passe incorrect → {@link InvalidPasswordException} (si implémenté)</li>
     *     <li>Connexion réussie → renvoie {@link AuthResponseDTO}</li>
     * </ul>
     * </p>
     *
     * @param loginDTO DTO contenant l'email et le mot de passe de l'utilisateur
     * @return ResponseEntity contenant {@link AuthResponseDTO} avec le JWT, refresh token, rôle et email
     */

    @Operation(
            summary = "Connexion d'un utilisateur",
            description = "Vérifie l'email et le mot de passe de l'utilisateur. Retourne JWT, refresh token et rôle."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connexion réussie",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Mot de passe incorrect",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody @Parameter(description = "Données de connexion de l'utilisateur", required = true) LoginDTO loginDTO
    ) {
        return ResponseEntity.ok(utilisateurService.login(loginDTO));
    }

}
