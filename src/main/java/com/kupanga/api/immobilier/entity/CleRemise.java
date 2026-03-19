package com.kupanga.api.immobilier.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cles_remises")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleRemise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Description de la clé ────────────────────────────────────────────────
    @Column(nullable = false)
    private String typeCle;         // ex : "Porte d'entrée", "Boîte aux lettres", "Garage"

    @Column(nullable = false)
    private Integer quantite;       // nombre d'exemplaires remis / restitués

    // ─── Relation ─────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etat_des_lieux_id", nullable = false)
    private EtatDesLieux etatDesLieux;
}
