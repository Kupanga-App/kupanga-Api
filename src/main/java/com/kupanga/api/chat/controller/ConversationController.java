package com.kupanga.api.chat.controller;

import com.kupanga.api.chat.dto.ConversationPageDTO;
import com.kupanga.api.chat.dto.ConversationSearchDTO;
import com.kupanga.api.chat.research.ConversationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conversations")
@Tag(name = "Conversation" , description = " gestion des conversations au niveau du chat")
public class ConversationController {

    private final ConversationSearchService conversationSearchService;

    // =========================================
    // RECHERCHER SES CONVERSATIONS
    // =========================================
    @Operation(
            summary = "Rechercher ses conversations",
            description = """
                    Retourne les conversations de l'utilisateur connecté, paginées et filtrables.
                    Chaque conversation correspond à un échange entre le propriétaire et le locataire
                    d'un bien donné.

                    **Filtres disponibles :**
                    - `nomDuBien` : recherche partielle insensible à la casse sur le titre du bien
                    - `lu` : `true` pour les conversations entièrement lues, `false` pour celles ayant des messages non lus

                    **Tri disponible :** `dernierMessage`, `nomDuBien`. Défaut : `dernierMessage DESC`
                    (conversation la plus récente en premier).

                    **Pagination :** défaut page `0`, taille `10`.

                    Un body vide `{}` ou absent retourne toutes les conversations paginées.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste paginée des conversations de l'utilisateur connecté",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "contenu": [
                                            {
                                                "bienId": 1,
                                                "bienTitre": "Appartement T3 centre-ville",
                                                "interlocuteurNom": "Sophie Martin",
                                                "interlocuteurEmail": "sophie.martin@email.com",
                                                "dernierMessage": "Bonjour, est-il possible de visiter samedi ?",
                                                "dernierMessageDate": "2026-05-17T14:32:00",
                                                "nonLus": 2
                                            }
                                        ],
                                        "pageActuelle": 0,
                                        "totalPages": 1,
                                        "totalElements": 1,
                                        "dernierePage": true,
                                        "premierePage": true
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Utilisateur non authentifié",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    { "error": "Accès non autorisé" }
                                    """)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Filtres, tri et pagination. Tous les champs sont optionnels.",
            required = false,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Conversations non lues uniquement",
                                    value = """
                                            {
                                                "lu": false,
                                                "page": 0,
                                                "size": 10,
                                                "sortBy": "dernierMessage",
                                                "sortDirection": "DESC"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Filtrer par bien",
                                    value = """
                                            {
                                                "nomDuBien": "Appartement",
                                                "page": 0,
                                                "size": 10
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Body vide — toutes les conversations",
                                    value = "{}"
                            )
                    }
            )
    )
    @PostMapping("/search")
    public ResponseEntity<ConversationPageDTO> rechercherConversations(
            @RequestBody(required = false) ConversationSearchDTO dto
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (dto == null) dto = new ConversationSearchDTO(
                null, null, null, null, null, null
        );

        return ResponseEntity.ok(conversationSearchService.rechercher(auth.getName(), dto));
    }
}
