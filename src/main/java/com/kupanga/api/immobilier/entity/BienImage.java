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

    @Column(nullable = false, length = 500)
    private String url;

    /** R1 : vignette de 400 px ; {@code null} pour une photo envoyée avant R1 (la photo d'origine sert alors). */
    @Column(name = "url_vignette", length = 500)
    private String urlVignette;

    /** Vignette, ou la photo elle-même si elle n'en a pas. */
    public String vignetteOuPhoto() {
        return urlVignette != null ? urlVignette : url;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;
}
