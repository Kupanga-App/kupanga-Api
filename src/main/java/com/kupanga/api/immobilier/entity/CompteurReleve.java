package com.kupanga.api.immobilier.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "compteur_releves")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteurReleve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Type et identification ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCompteur typeCompteur;      // EAU_FROIDE, GAZ, ELECTRICITE_HP…

    private String numeroCompteur;          // numéro de série / PDL / PCE

    // ─── Index relevé ─────────────────────────────────────────────────────────
    @Column(nullable = false)
    private Double index;                   // valeur du compteur au moment de l'EDL

    private String unite;                   // "m³", "kWh" — rempli automatiquement

    // ─── Relation ─────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etat_des_lieux_id", nullable = false)
    private EtatDesLieux etatDesLieux;
}
