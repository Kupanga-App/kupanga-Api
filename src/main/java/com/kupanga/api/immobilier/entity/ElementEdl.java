package com.kupanga.api.immobilier.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "elements_edl")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementEdl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Type d'élément ───────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeElement typeElement;    // MUR, SOL, PLAFOND, FENETRE…

    // ─── État constaté ────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatElement etatElement;    // BON, USAGE_NORMAL, MAUVAIS, HORS_SERVICE

    // ─── Description et observation ───────────────────────────────────────────
    private String description;         // ex : "Parquet chêne massif", "Peinture blanche"

    @Column(columnDefinition = "TEXT")
    private String observation;         // ex : "Rayure ~10 cm en bord de fenêtre"

    // ─── Relation ─────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_id", nullable = false)
    private PieceEdl piece;
}
