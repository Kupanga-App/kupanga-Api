package com.kupanga.api.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@Builder
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    private Role role; // PROPRIETAIRE, LOCATAIRE, ADMIN

    // relations
    @OneToMany(mappedBy = "proprietaire")
    private List<Bien> biensProprietes;

    @OneToMany(mappedBy = "locataire")
    private List<Bien> biensLoues;

    @OneToMany(mappedBy = "destinataire")
    private List<Message> messagesRecus;

    @OneToMany(mappedBy = "expediteur")
    private List<Message> messagesEnvoyes;
}
