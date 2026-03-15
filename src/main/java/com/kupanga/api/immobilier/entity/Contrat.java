package com.kupanga.api.immobilier.entity;

import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrats")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Informations du contrat ──────────────────────────────────────────────
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer   dureeBailMois;
    private Double    loyerMensuel;
    private Double    chargesMensuelles;
    private Double    depotGarantie;
    private String    adresseBien;

    // ─── Stockage PDF ─────────────────────────────────────────────────────────
    @Column(length = 500)
    private String urlPdf;

    // ─── Signatures ───────────────────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String signatureProprietaire;

    @Column(columnDefinition = "TEXT")
    private String signatureLocataire;

    private LocalDateTime dateSignatureProprietaire;
    private LocalDateTime dateSignatureLocataire;

    // ─── Token signature locataire ────────────────────────────────────────────
    @Column(unique = true)
    private String        tokenSignature;
    private LocalDateTime tokenExpiration;

    // ─── Statut ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutContrat statut;

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
}