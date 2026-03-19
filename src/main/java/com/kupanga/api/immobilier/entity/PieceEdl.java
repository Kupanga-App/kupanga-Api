package com.kupanga.api.immobilier.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "pieces_edl")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PieceEdl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Identification de la pièce ───────────────────────────────────────────
    @Column(nullable = false)
    private String nomPiece;        // ex : "Salon", "Chambre 1", "Cuisine"

    private Integer ordre;          // ordre d'affichage dans le PDF (1, 2, 3…)

    @Column(columnDefinition = "TEXT")
    private String observations;    // remarques libres sur la pièce entière

    // ─── Relations ────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etat_des_lieux_id", nullable = false)
    private EtatDesLieux etatDesLieux;

    @OneToMany(mappedBy = "piece", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private Set<ElementEdl> elements = new HashSet<>();
}
