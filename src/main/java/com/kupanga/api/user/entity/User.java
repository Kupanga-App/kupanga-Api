package com.kupanga.api.user.entity;

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
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @Column(name = "prenom")
    private String firstName;


    @Column(name = "nom")
    private String lastName;

    @Column(name = "email")
    private String mail;

    @Column(name = "motDePasse")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role; // PROPRIETAIRE, LOCATAIRE, ADMIN

    @Column(name = "A_completer_profil")
    private Boolean hasCompleteProfil = false;

    @Column(name = "url_photo_profil")
    private String urlProfile;

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
