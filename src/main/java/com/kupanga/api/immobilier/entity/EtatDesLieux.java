package com.kupanga.api.immobilier.entity;

import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "etats_des_lieux")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtatDesLieux {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Type et date ──────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEtat type;                  // ENTREE | SORTIE

    @Column(nullable = false)
    private LocalDate dateRealisation;

    private LocalTime heureRealisation;

    // ─── Statut ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEdl statut;

    // ─── Observations générales ───────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String observations;

    // ─── PDF final ────────────────────────────────────────────────────────────
    @Column(length = 500)
    private String urlPdf;

    // ─── Signatures ───────────────────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String signatureProprietaire;

    private LocalDateTime dateSignatureProprietaire;

    @Column(columnDefinition = "TEXT")
    private String signatureLocataire;

    private LocalDateTime dateSignatureLocataire;

    // ─── Token signature locataire ────────────────────────────────────────────
    @Column(unique = true)
    private String tokenSignature;

    private LocalDateTime tokenExpiration;

    // ─── Audit ────────────────────────────────────────────────────────────────
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Relations ────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private User proprietaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locataire_id", nullable = false)
    private User locataire;

    @OneToMany(mappedBy = "etatDesLieux", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    private Set<PieceEdl> pieces = new HashSet<>();

    @OneToMany(mappedBy = "etatDesLieux", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CompteurReleve> compteurs = new HashSet<>();

    @OneToMany(mappedBy = "etatDesLieux", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CleRemise> cles = new HashSet<>();
}