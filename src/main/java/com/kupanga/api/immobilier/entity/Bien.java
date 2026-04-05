package com.kupanga.api.immobilier.entity;

import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    // ─── Informations générales ───────────────────────────────────────────────
    private String titre;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private String description;

    @Enumerated(EnumType.STRING)
    private TypeBien typeBien;

    // ─── Localisation ─────────────────────────────────────────────────────────
    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point localisation;

    // ─── Caractéristiques physiques ───────────────────────────────────────────
    private Double          surfaceHabitable;       // en m²
    private Integer         nombrePieces;
    private Integer         nombreChambres;
    private Integer         etage;                  // 0 = rez-de-chaussée
    private Boolean         ascenseur;
    private Integer         anneeConstruction;

    @Enumerated(EnumType.STRING)
    private ModeChauffage   modeChauffage;

    // ─── Diagnostic énergétique ───────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private ClasseEnergie   classeEnergie;          // A, B, C, D, E, F, G

    @Enumerated(EnumType.STRING)
    private ClasseGes       classeGes;              // A, B, C, D, E, F, G

    // ─── Conditions de location ───────────────────────────────────────────────
    private Double          loyerMensuel;
    private Double          chargesMensuelles;
    private Double          depotGarantie;
    private Boolean         meuble;                 // true = meublé
    private Boolean         colocation;             // true = colocation possible
    private LocalDate       disponibleDe;           // date de disponibilité

    // ─── Audit ────────────────────────────────────────────────────────────────
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Relations ────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id")
    private User proprietaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locataire_id")
    private User locataire;

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Contrat> contrats = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Quittance> quittances = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<EtatDesLieux> etatsDesLieux = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Document> documents = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<BienImage> images = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<BienPoi> pois = new HashSet<>();

    @OneToMany(mappedBy = "bien", cascade = CascadeType.ALL)
    private Set<Conversation> conversations = new HashSet<>();
}