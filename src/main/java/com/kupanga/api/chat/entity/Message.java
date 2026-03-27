package com.kupanga.api.chat.entity;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Contenu ──────────────────────────────────────────────────────────────
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    // ─── Statut lecture ───────────────────────────────────────────────────────
    @Column(nullable = false)
    private Boolean lu = false;

    // ─── Audit ────────────────────────────────────────────────────────────────
    @CreationTimestamp
    private LocalDateTime createdAt;

    // ─── Relations ────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private User expediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private User destinataire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_id")
    private Bien bien;                  // contexte optionnel : proprio ↔ locataire sur un bien
}
