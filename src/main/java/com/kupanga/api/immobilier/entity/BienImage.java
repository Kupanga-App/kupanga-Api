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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;
}
