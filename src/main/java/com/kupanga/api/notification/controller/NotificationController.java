package com.kupanga.api.notification.controller;

import com.kupanga.api.notification.dto.AppNotificationDTO;
import com.kupanga.api.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications applicatives — persistées et temps réel")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Récupérer les notifications non lues",
            description = """
                    Retourne toutes les notifications non lues de l'utilisateur connecté,
                    triées par date décroissante. À appeler au chargement de l'application
                    pour afficher les notifications reçues pendant la déconnexion.
                    """
    )
    @GetMapping
    public ResponseEntity<List<AppNotificationDTO>> getNonLues() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(notificationService.getNonLues(auth.getName()));
    }

    @Operation(
            summary = "Marquer une notification comme lue",
            description = "Marque la notification identifiée par son id comme lue pour l'utilisateur connecté."
    )
    @PatchMapping("/{id}/lire")
    public ResponseEntity<Void> marquerLue(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        notificationService.marquerLue(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Marquer toutes les notifications comme lues",
            description = "Marque l'ensemble des notifications non lues de l'utilisateur connecté comme lues."
    )
    @PatchMapping("/lire-toutes")
    public ResponseEntity<Void> marquerToutesLues() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        notificationService.marquerToutesLues(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
