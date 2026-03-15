package com.kupanga.api.immobilier.entity;

import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "biens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point localisation;

    @Enumerated(EnumType.STRING)
    private TypeBien typeBien;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id")
    private User proprietaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locataire_id")
    private User locataire;

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Contrat> contrats;

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Quittance> quittances;

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<EtatDesLieux> etatsDesLieux;

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Document> documents;

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BienImage> images;
}