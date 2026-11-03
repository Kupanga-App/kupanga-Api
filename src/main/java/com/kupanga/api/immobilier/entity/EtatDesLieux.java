package com.kupanga.api.immobilier.entity;

import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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

    // B6 : verrou optimiste — deux signatures (ou relances) simultanées ne peuvent pas s'écraser
    @Version
    private Long version;

    // ─── Type et date ──────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEtat type;                  // ENTREE | SORTIE

    @Column(nullable = false)
    private LocalDate dateRealisation;

    private LocalTime heureRealisation;

    // ─── J5 : juridiction figée à la création (modèle du PDF inchangé si le profil évolue) ─
    @Enumerated(EnumType.STRING)
    @Column(name = "pays", length = 2, nullable = false, updatable = false)
    private Pays pays;

    /** Version du modèle de document utilisé (ex. {@code fr-v1}). */
    @Column(name = "modele_version", length = 20, nullable = false, updatable = false)
    private String modeleVersion;

    // ─── Statut ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEdl statut;

    // ─── Observations générales ───────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String observations;

    // ─── PDF final ────────────────────────────────────────────────────────────
    /** Clé de l'objet dans le bucket MinIO privé (jamais une URL publique — P0-7). */
    @Column(name = "cle_pdf", length = 500)
    private String clePdf;

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

    // ─── Ordre d'affichage (PDF) ──────────────────────────────────────────────
    // Méthodes sans préfixe « get » : ni Hibernate ni MapStruct ne les prennent pour des propriétés.

    /** Pièces dans l'ordre d'affichage : {@code ordre}, puis ordre de saisie (id). */
    public List<PieceEdl> piecesTriees() {
        return trier(pieces, Comparator.comparing(PieceEdl::getOrdre, Comparator.nullsLast(Comparator.<Integer>naturalOrder()))
                .thenComparing(PieceEdl::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder())));
    }

    /** Compteurs dans l'ordre de saisie. */
    public List<CompteurReleve> compteursTries() {
        return trier(compteurs, Comparator.comparing(CompteurReleve::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder())));
    }

    /** Clés dans l'ordre de saisie. */
    public List<CleRemise> clesTriees() {
        return trier(cles, Comparator.comparing(CleRemise::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder())));
    }

    /** Tri stable : à clé égale (ou absente), l'ordre d'itération de l'ensemble est conservé. */
    static <T> List<T> trier(Set<T> elements, Comparator<T> ordre) {
        return elements == null ? List.of() : elements.stream().sorted(ordre).toList();
    }
}
