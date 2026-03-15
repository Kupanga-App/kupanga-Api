package com.kupanga.api.immobilier.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bien_poi", indexes = {
        @Index(name = "idx_bien_poi_bien_id",  columnList = "bien_id"),
        @Index(name = "idx_bien_poi_type",     columnList = "poi_type"),
        @Index(name = "idx_bien_poi_present",  columnList = "bien_id, poi_type, present")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BienPoi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;

    @Enumerated(EnumType.STRING)
    @Column(name = "poi_type", nullable = false)
    private PoiType poiType;

    @Column(nullable = false)
    private Boolean present;          // true = POI trouvé dans le rayon

    @Column(nullable = false)
    private Double rayonMetres;       // rayon utilisé pour la recherche

    @Column(nullable = false)
    private Integer nombreTrouve;     // combien de POI trouvés

    @CreationTimestamp
    private LocalDateTime calculeLe;  // date du calcul
}
