package com.kupanga.api.immobilier.entity;

import com.kupanga.api.utilisateur.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "contrats")
@Getter
@Setter
@Builder
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Double loyerMensuel;
    private String urlPdf; // lien vers MinIO

    @ManyToOne
    @JoinColumn(name = "bien_id")
    private Bien bien;

    @ManyToOne
    @JoinColumn(name = "proprietaire_id")
    private Utilisateur proprietaire;

    @ManyToOne
    @JoinColumn(name = "locataire_id")
    private Utilisateur locataire;
}
