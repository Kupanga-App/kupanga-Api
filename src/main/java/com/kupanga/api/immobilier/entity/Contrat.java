package com.kupanga.api.immobilier.entity;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
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

    // B6 : verrou optimiste — deux signatures (ou relances) simultanées ne peuvent pas s'écraser
    @Version
    private Long version;

    // ─── Informations du contrat ──────────────────────────────────────────────
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer   dureeBailMois;
    @Column(precision = 12, scale = 2)
    private BigDecimal    loyerMensuel;
    @Column(precision = 12, scale = 2)
    private BigDecimal    chargesMensuelles;
    @Column(precision = 12, scale = 2)
    private BigDecimal    depotGarantie;
    private String    adresseBien;

    // ─── Juridiction figée à la création (J3, §4bis) ─────────────────────────
    // Copiées du bien et du profil : le document ne change jamais si la configuration évolue.
    @Enumerated(EnumType.STRING)
    @Column(name = "pays", length = 2, nullable = false, updatable = false)
    private Pays      pays;

    @Enumerated(EnumType.STRING)
    @Column(name = "devise", length = 3, nullable = false, updatable = false)
    private Devise    devise;

    /** Version du modèle de document utilisé (ex. {@code fr-v1}). */
    @Column(name = "modele_version", length = 20, nullable = false, updatable = false)
    private String    modeleVersion;


    // ─── Stockage PDF ─────────────────────────────────────────────────────────
    /** Clé de l'objet dans le bucket MinIO privé (jamais une URL publique — P0-7). */
    @Column(name = "cle_pdf", length = 500)
    private String clePdf;

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