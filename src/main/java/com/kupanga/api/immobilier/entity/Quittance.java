package com.kupanga.api.immobilier.entity;

import com.kupanga.api.utilisateur.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quittances")
@Getter
@Setter
@Builder
public class Quittance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double montant;
    private String mois;
    private Integer annee;
    private String urlPdf;

    @ManyToOne
    @JoinColumn(name = "bien_id")
    private Bien bien;

    @ManyToOne
    @JoinColumn(name = "locataire_id")
    private User locataire;
}
