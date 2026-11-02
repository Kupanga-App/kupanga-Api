package com.kupanga.api.email.file;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * M2 : e-mail en attente ou envoyé (« transactional outbox »). La ligne est écrite <b>dans la même transaction</b>
 * que l'action qui demande l'e-mail : rien n'est envoyé si elle est annulée, rien n'est perdu si l'application ou
 * le serveur SMTP tombent ensuite (nouveaux essais par {@code EnvoyeurEmails}).
 */
@Entity
@Table(name = "emails_sortants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEnFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeEmail type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrioriteEmail priorite;

    @Column(nullable = false, length = 320)
    private String destinataire;

    @Column(length = 500)
    private String sujet;

    /** Effacé dès l'envoi (il peut contenir un lien de connexion, de reset ou de signature). */
    @Column(columnDefinition = "TEXT")
    private String html;

    @Column(columnDefinition = "TEXT")
    private String texte;

    @Builder.Default
    @org.hibernate.annotations.BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "emails_sortants_pieces_jointes", joinColumns = @JoinColumn(name = "email_id"))
    @OrderColumn(name = "ordre")
    private List<PdfAJoindre> piecesJointes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEmail statut;

    @Column(nullable = false)
    private int tentatives;

    @Column(name = "prochain_essai", nullable = false)
    private Instant prochainEssai;

    @Column(name = "verrou_jusqua")
    private Instant verrouJusqua;

    /** Classe de l'erreur et code SMTP seulement : ni adresse, ni sujet, ni contenu (W10). */
    @Column(name = "derniere_erreur", length = 200)
    private String derniereErreur;

    /** Au-delà, le lien contenu a expiré : l'e-mail passe en {@code PERIME} au lieu d'être envoyé. */
    @Column(name = "expire_le")
    private Instant expireLe;

    @Column(name = "envoye_le")
    private Instant envoyeLe;

    @Column(name = "contenu_efface", nullable = false)
    private boolean contenuEfface;
}
