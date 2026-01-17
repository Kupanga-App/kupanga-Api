package com.kupanga.api.chat.entity;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contenu;
    private LocalDateTime dateEnvoi;
    private Boolean lu;

    @ManyToOne
    @JoinColumn(name = "expediteur_id")
    private Utilisateur expediteur;

    @ManyToOne
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;

    @ManyToOne
    @JoinColumn(name = "bien_id")
    private Bien bien;
}
