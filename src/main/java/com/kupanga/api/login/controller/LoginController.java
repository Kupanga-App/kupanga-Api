package com.kupanga.api.login.controller;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.login.dto.AuthResponseDTO;
import com.kupanga.api.login.dto.LoginDTO;
import com.kupanga.api.login.service.LoginService;
import com.kupanga.api.utilisateur.dto.formDTO.UserFormDTO;
import com.kupanga.api.utilisateur.dto.readDTO.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;

    // =========================================
    // CREATION UTILISATEUR
    // =========================================
    @Operation(
            summary = "Créer un nouvel utilisateur",
            description = "Crée un utilisateur avec un email et un rôle. Retourne le DTO de l'utilisateur créé."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Utilisateur créé avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "id": 1,
                                        "email": "user@example.com",
                                        "nom": "John",
                                        "prenom": "Doe",
                                        "role": "ROLE_LOCATAIRE"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Un utilisateur avec cet email existe déjà",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Utilisateur déjà existant"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Rôle invalide fourni",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Rôle invalide"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/create-count")
    public ResponseEntity<UserDTO> creationCompte(@RequestBody UserFormDTO userFormDTO)
            throws UserAlreadyExistsException {
        return ResponseEntity.ok(loginService.creationUtilisateur(userFormDTO.email(), userFormDTO.role()));
    }

    // =========================================
    // LOGIN
    // =========================================
    @Operation(
            summary = "Connexion d'un utilisateur",
            description = "Vérifie l'email et le mot de passe de l'utilisateur. Retourne access token et refresh token."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Connexion réussie",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "refreshToken": "d5f4c3b2a1..."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Utilisateur non trouvé",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Utilisateur non trouvé"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Mot de passe incorrect",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Mot de passe incorrect"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        return ResponseEntity.ok(loginService.login(loginDTO, response));
    }

    // =========================================
    // REFRESH TOKEN
    // =========================================
    @Operation(
            summary = "Rafraîchir le token",
            description = "Vérifie si l'access token est expiré et utilise le refresh token pour générer un nouveau token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token JWT rafraîchi avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "refreshToken": "d5f4c3b2a1..."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de rafraîchissement invalide ou expiré",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Refresh token invalide ou expiré"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Utilisateur associé au refresh token non trouvé",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Utilisateur non trouvé"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@CookieValue("refreshToken") String refreshToken) {
        return ResponseEntity.ok(loginService.refresh(refreshToken));
    }

    // =========================================
    // LOGOUT
    // =========================================
    @Operation(
            summary = "Déconnexion de l'utilisateur",
            description = "Supprime le refresh token côté serveur et efface le cookie associé."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Déconnexion réussie",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "Utilisateur déconnecté avec succès"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token manquant",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Refresh token absent"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                         HttpServletResponse response) {
        return ResponseEntity.ok(loginService.logout(refreshToken, response));
    }
}
