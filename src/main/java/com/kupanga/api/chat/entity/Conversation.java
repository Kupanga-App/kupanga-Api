package com.kupanga.api.chat.entity;

import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.user.utils.EmailUtils;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name ="conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Bien concerné ───────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_id")
    private Bien bien;

    private String emailExpediteur;
    private String emailDestinataire;

    // ─── Messages ────────────────────────────────────────────────
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<Message> messages;

    // ─── Dernier message (optimisation UI) ────────────────────────
    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    // ─── Audit ───────────────────────────────────────────────────
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** A10 : e-mails stockés en minuscules, comme {@code utilisateurs.email}. */
    @PrePersist
    @PreUpdate
    void normaliserEmails() {
        this.emailExpediteur = EmailUtils.normaliser(this.emailExpediteur);
        this.emailDestinataire = EmailUtils.normaliser(this.emailDestinataire);
    }
}
