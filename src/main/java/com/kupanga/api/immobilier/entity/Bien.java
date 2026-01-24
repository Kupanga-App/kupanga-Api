package com.kupanga.api.immobilier.entity;

import com.kupanga.api.utilisateur.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "biens")
@Getter
@Setter
@Builder
public class Bien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;
    private String adresse;
    private String ville;
    private String codePostal;
    private Double latitude;
    private Double longitude;
    private String description;

    @ManyToOne
    @JoinColumn(name = "proprietaire_id")
    private User proprietaire;

    @ManyToOne
    @JoinColumn(name = "locataire_id")
    private User locataire;

    @OneToMany(mappedBy = "bien")
    private List<Contrat> contrats;

    @OneToMany(mappedBy = "bien")
    private List<Quittance> quittances;

    @OneToMany(mappedBy = "bien")
    private List<EtatDesLieux> etatsDesLieux;

    @OneToMany(mappedBy = "bien")
    private List<Document> documents;
}
