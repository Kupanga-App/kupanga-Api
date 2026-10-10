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
@Table(name = "quittances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quittance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // B6 : verrou optimiste — deux signatures (ou relances) simultanées ne peuvent pas s'écraser
    @Version
    private Long version;

    // ─── Période concernée ────────────────────────────────────────────────────
    private String mois;
    private Integer annee;

    // ─── Détail financier ─────────────────────────────────────────────────────
    @Column(precision = 12, scale = 2)
    private BigDecimal loyerMensuel;        // loyer hors charges
    @Column(precision = 12, scale = 2)
    private BigDecimal chargesMensuelles;   // charges mensuelles
    @Column(precision = 12, scale = 2)
    private BigDecimal montantTotal;        // loyerMensuel + chargesMensuelles

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


    // ─── Paiement ─────────────────────────────────────────────────────────────
    private LocalDate datePaiement;     // date effective d'encaissement
    private LocalDate dateEcheance;     // date limite de paiement (ex : 5 du mois)

    // ─── Statut ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutQuittance statut;

    // ─── Signature propriétaire ───────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String signatureProprietaire;

    private LocalDateTime dateSignatureProprietaire;

    // ─── PDF ──────────────────────────────────────────────────────────────────
    /** Clé de l'objet dans le bucket MinIO privé (jamais une URL publique — P0-7). */
    @Column(name = "cle_pdf", length = 500)
    private String clePdf;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_id")
    private Contrat contrat;
}