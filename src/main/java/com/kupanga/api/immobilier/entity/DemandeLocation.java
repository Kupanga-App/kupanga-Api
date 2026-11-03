package com.kupanga.api.immobilier.entity;

import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Demande de location : le propriétaire propose son bien à un candidat (une personne qui lui a écrit au sujet
 * de ce bien). Le bien n'est assigné qu'à l'acceptation par le locataire. Une seule demande en attente par bien
 * (index unique partiel, V53).
 */
@Entity
@Table(name = "demandes_location")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeLocation {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDemandeLocation statut;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_expiration", nullable = false, updatable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    /** Verrou optimiste : une acceptation et une annulation simultanées ne peuvent pas réussir toutes les deux. */
    @Version
    private Long version;

    /** Statut affiché : une demande en attente dont le délai est dépassé est expirée. */
    public StatutDemandeLocation statutA(LocalDateTime maintenant) {
        return statut == StatutDemandeLocation.EN_ATTENTE && !maintenant.isBefore(dateExpiration)
                ? StatutDemandeLocation.EXPIREE
                : statut;
    }
}
