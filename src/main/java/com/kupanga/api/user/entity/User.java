package com.kupanga.api.user.entity;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.chat.entity.Message;
import com.kupanga.api.user.utils.EmailUtils;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(name = "mot_de_passe")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "a_completer_profil")
    private Boolean hasCompleteProfil = false;

    @Column(name = "url_photo_profil")
    private String urlProfile;

    @Column(name = "google_id")
    private String googleId;

    /** A14 : faux tant que le lien envoyé à l'inscription n'a pas été ouvert ; connexion refusée d'ici là. */
    @Column(name = "email_verifie", nullable = false)
    private boolean emailVerifie;

    /** B12 : compte supprimé qui avait des baux : identité effacée, ligne gardée pour les documents. */
    @Column(name = "anonymise", nullable = false)
    private boolean anonymise;

    @Column(name = "date_anonymisation")
    private LocalDateTime dateAnonymisation;

    // relations (B12 : aucune cascade, supprimer un compte ne supprime ni ses biens ni les messages des autres)
    @OneToMany(mappedBy = "proprietaire" , fetch = FetchType.LAZY)
    private List<Bien> biensProprietes;

    @OneToMany(mappedBy = "locataire", fetch = FetchType.LAZY)
    private List<Bien> biensLoues;

    @OneToMany(mappedBy = "destinataire", fetch = FetchType.LAZY)
    private List<Message> messagesRecus;

    @OneToMany(mappedBy = "expediteur" ,fetch = FetchType.LAZY)
    private List<Message> messagesEnvoyes;

    /** A10 : l'e-mail est toujours stocké en minuscules, quel que soit le chemin de création. */
    @PrePersist
    @PreUpdate
    void normaliserMail() {
        this.mail = EmailUtils.normaliser(this.mail);
    }
}
