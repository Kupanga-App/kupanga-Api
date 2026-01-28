package com.kupanga.api.authentification.controller;

import com.kupanga.api.exception.business.UserAlreadyExistsException;
import com.kupanga.api.authentification.dto.AuthResponseDTO;
import com.kupanga.api.authentification.dto.LoginDTO;
import com.kupanga.api.authentification.service.AuthService;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.authentification.dto.CompleteProfileResponseDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

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
    @PostMapping("/register")
    public ResponseEntity<UserDTO> creationCompte(@Valid @RequestBody LoginDTO loginDTO)
            throws UserAlreadyExistsException {
        return ResponseEntity.ok(authService.creationUtilisateur(loginDTO));
    }

    // =========================================
    // COMPLETER LE PROFIL UTILISATEUR
    // =========================================
    @Operation(
            summary = "Compléter le profil utilisateur",
            description = "Permet à un utilisateur de compléter son profil en fournissant les informations " +
                    "nécessaires telles que le nom, prénom, email, et autres champs définis dans le DTO. " +
                    "Retourne le profil utilisateur mis à jour et le reconnecte automatiquement"
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Profil utilisateur complété avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CompleteProfileResponseDTO.class),
                            examples = @ExampleObject(value = """
                {
                  "userDTO": {
                    "id": 21,
                    "firstName": "John",
                    "lastName": "Doe",
                    "mail": "user@example.com",
                    "password": "$2a$10$hASvFqpTZAgWQNb.nPKVB.9uejUfwitZ99cn/uULT4f678usM6FVy",
                    "role": "ROLE_LOCATAIRE",
                    "hasCompleteProfil": true,
                    "urlProfil": null
                  },
                  "authResponseDTO": {
                    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9MT0NBVEFJUkQiLCJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzY5NjMxNTYzLCJleHAiOjE3Njk2MzE4NjN9._M4sPiCCxW934w95XEw8_Ix05JsnpOcQlbX1bNHN2GA"
                  }
                }
                """)
                    )
            ),


            @ApiResponse(
                    responseCode = "400",
                    description = "Requête invalide : certains champs du formulaire sont manquants ou incorrects",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                            {
                                "error": "Champs obligatoires manquants ou invalides"
                            }
                            """)
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé : l'utilisateur n'est pas autorisé à compléter ce profil",
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
    @PostMapping("/completeProfile")
    public ResponseEntity<CompleteProfileResponseDTO> completeProfile(@Valid @RequestBody UserFormDTO userFormDTO , HttpServletResponse response) {
        return ResponseEntity.ok(authService.completeProfil(userFormDTO , response));
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
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        return ResponseEntity.ok(authService.login(loginDTO, response));
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
        return ResponseEntity.ok(authService.refresh(refreshToken));
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
        return ResponseEntity.ok(authService.logout(refreshToken, response));
    }

    // =========================================
    // FORGOT PASSWORD
    // =========================================

    @Operation(
            summary = "Réinitialisation du mot de passe",
            description = "Génère un token temporaire pour réinitialiser le mot de passe et envoie un email contenant le lien de réinitialisation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email de réinitialisation envoyé avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "message": "Lien de réinitialisation envoyé à l'adresse email"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email inexistant ou invalide",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Utilisateur introuvable"
                                }
                                """)
                    )
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        return ResponseEntity.ok(authService.forgotPassword(email));
    }

    // =========================================
    // RESET PASSWORD
    // =========================================

    @Operation(
            summary = "Mise à jour du mot de passe",
            description = "Permet à l'utilisateur de réinitialiser son mot de passe à partir du token reçu par email. " +
                    "Le token est valide pendant 10 minutes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Mot de passe mis à jour avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "message": "Mot de passe mis à jour"
                                }
                                """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token invalide ou expiré",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                    "error": "Token expiré ou invalide"
                                }
                                """)
                    )
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token,
                                                @RequestParam String newPassword) {
        return ResponseEntity.ok(authService.resetPassword(token, newPassword));
    }

}
