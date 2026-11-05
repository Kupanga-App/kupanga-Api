package com.kupanga.api.immobilier.entity;

import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Location d'un bien : réservée à l'acceptation d'une demande de location, active à la signature du bail.
 * Au plus une location réservée ou active par bien (index unique partiel, V54). {@code Bien.locataire} reste
 * le locataire de la location active.
 */
@Entity
@Table(name = "locations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bien_id", nullable = false)
    private Bien bien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proprietaire_id", nullable = false)
    private User proprietaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "locataire_id", nullable = false)
    private User locataire;

    /** Demande acceptée qui a créé la réservation (absente pour les locations reprises par V54). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private DemandeLocation demande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutLocation statut;

    @Column(name = "date_reservation", nullable = false, updatable = false)
    private LocalDateTime dateReservation;

    /** Fin de la réservation si le bail n'est pas signé avant. */
    @Column(name = "date_expiration", nullable = false, updatable = false)
    private LocalDateTime dateExpiration;

    /** Signature du bail (passage en ACTIVE). */
    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    /** Désistement, retrait, expiration ou fin de la location. */
    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    // ─── Fin de bail (V55) ───────────────────────────────────────────────

    /** Date de fin annoncée par le congé (null : pas de congé en cours). */
    @Column(name = "date_fin_prevue")
    private java.time.LocalDate dateFinPrevue;

    @Enumerated(EnumType.STRING)
    @Column(name = "conge_donne_par", length = 20)
    private PartieLocation congeDonnePar;

    @Column(name = "date_conge")
    private LocalDateTime dateConge;

    /** Dépôt de garantie restitué à la fin du bail (montant dans la devise du bail). */
    @Column(name = "depot_restitue", precision = 12, scale = 2)
    private java.math.BigDecimal depotRestitue;

    /** Motif de la retenue sur le dépôt (obligatoire si tout n'est pas restitué). */
    @Column(name = "motif_retenue", length = 500)
    private String motifRetenue;

    /** Verrou optimiste : désistement, retrait et signature du bail simultanés ne réussissent pas tous. */
    @Version
    private Long version;

    /** Statut affiché : une réservation dont l'échéance est passée est expirée. */
    public StatutLocation statutA(LocalDateTime maintenant) {
        return statut == StatutLocation.RESERVEE && !maintenant.isBefore(dateExpiration)
                ? StatutLocation.EXPIREE
                : statut;
    }

    /** Réservation encore valable à cet instant. */
    public boolean estReserveeA(LocalDateTime maintenant) {
        return statutA(maintenant) == StatutLocation.RESERVEE;
    }
}
