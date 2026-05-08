package com.kupanga.api.immobilier.entity;

import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    // ─── Période concernée ────────────────────────────────────────────────────
    private String mois;
    private Integer annee;

    // ─── Détail financier ─────────────────────────────────────────────────────
    private Double loyerMensuel;        // loyer hors charges
    private Double chargesMensuelles;   // charges mensuelles
    private Double montantTotal;        // loyerMensuel + chargesMensuelles

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
    @Column(length = 500)
    private String urlPdf;

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