package com.kupanga.api.immobilier.entity;

import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.Pays;
import com.kupanga.api.chat.entity.Conversation;
import com.kupanga.api.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // ─── J4 (C1) : adresse congolaise (vide si le pays ne l'utilise pas) ──────
    @Column(length = 100) private String commune;
    @Column(length = 100) private String quartier;
    @Column(length = 150) private String avenue;
    @Column(length = 50)  private String numeroParcelle;
    @Column(length = 255) private String pointDeRepere;

    /** J1 : code ISO du pays ; sélectionne le profil de juridiction. */
    @Enumerated(EnumType.STRING)
    @Column(name = "pays", length = 2, nullable = false)
    private Pays pays;
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
    // J3 : montants exacts (NUMERIC(12,2)) dans la devise du bien
    @Column(precision = 12, scale = 2)
    private BigDecimal      loyerMensuel;
    @Column(precision = 12, scale = 2)
    private BigDecimal      chargesMensuelles;
    @Column(precision = 12, scale = 2)
    private BigDecimal      depotGarantie;

    /** J3 : devise des montants, parmi celles du profil de juridiction du pays. */
    @Enumerated(EnumType.STRING)
    @Column(name = "devise", length = 3, nullable = false)
    private Devise          devise;
    private Boolean         meuble;                 // true = meublé
    private Boolean         colocation;             // true = colocation possible
    private LocalDate       disponibleDe;           // date de disponibilité

    // ─── Audit ────────────────────────────────────────────────────────────────
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Archivage (B12) ──────────────────────────────────────────────────────
    /** Un bien n'est jamais supprimé : archivé, il sort de la recherche publique et passe en lecture seule. */
    @Column(name = "archive", nullable = false)
    private boolean archive;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    // ─── Relations ────────────────────────────────────────────────────────────
    // B12 : pas de cascade vers les baux, quittances, EDL et conversations (documents à conserver)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id")
    private User proprietaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locataire_id")
    private User locataire;

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY)
    private Set<Contrat> contrats = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY)
    private Set<Quittance> quittances = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY)
    private Set<EtatDesLieux> etatsDesLieux = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Document> documents = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<BienImage> images = new HashSet<>();

    @OneToMany(mappedBy = "bien", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<BienPoi> pois = new HashSet<>();

    @OneToMany(mappedBy = "bien")
    private Set<Conversation> conversations = new HashSet<>();

    /**
     * J4 : adresse sur une ligne pour les documents (bail, quittance, EDL), sans « null » pour les champs vides
     * (code postal en RDC, quartier en France…). Ex. « 12 rue des Tests, 44000 Nantes » ou
     * « N° 12, avenue Kasa-Vubu, quartier Matonge, commune de Kalamu, Kinshasa ».
     */
    public String adresseComplete() {
        return Stream.of(
                        adresse,
                        prefixe("avenue ", avenue),
                        prefixe("parcelle n° ", numeroParcelle),
                        prefixe("quartier ", quartier),
                        prefixe("commune de ", commune),
                        Stream.of(codePostal, ville).filter(Bien::renseigne).collect(Collectors.joining(" ")))
                .filter(Bien::renseigne)
                .collect(Collectors.joining(", "));
    }

    private static String prefixe(String prefixe, String valeur) {
        return renseigne(valeur) ? prefixe + valeur : null;
    }

    private static boolean renseigne(String texte) {
        return texte != null && !texte.isBlank();
    }
}
