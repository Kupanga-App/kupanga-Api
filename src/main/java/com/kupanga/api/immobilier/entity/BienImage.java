package com.kupanga.api.immobilier.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bien_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BienImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** S2 : clé de la photo (1600 px) dans le bucket des photos de biens ; l'URL est construite à la lecture. */
    @Column(name = "cle")
    private String cle;

    /** S2 / R1 : clé de la vignette de 400 px ; {@code null} pour une photo envoyée avant R1. */
    @Column(name = "cle_vignette")
    private String cleVignette;

    /**
     * Ancienne URL complète (avant S2), gardée pour la transition (V51) ; jamais écrite pour une nouvelle photo.
     * Lue seulement si la clé est absente (URL d'un format inattendu).
     */
    @Column(length = 500)
    private String url;

    /** Ancienne URL de la vignette (avant S2), même rôle que {@link #url}. */
    @Column(name = "url_vignette", length = 500)
    private String urlVignette;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;
}
