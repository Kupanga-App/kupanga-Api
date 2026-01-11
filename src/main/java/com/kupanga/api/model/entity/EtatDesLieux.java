package com.kupanga.api.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "etats_des_lieux")
@Getter
@Setter
@Builder
public class EtatDesLieux {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TypeEtat type; // ENTREE / SORTIE

    private String observations;
    private String urlPdf; // PDF final
    private LocalDate dateRealisation;

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
