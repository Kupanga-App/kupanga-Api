package com.kupanga.api.utilisateur.entity;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.chat.entity.Message;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
